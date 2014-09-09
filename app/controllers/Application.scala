package controllers

import java.io.File
import java.io.FileOutputStream
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants.THURSDAY
import org.joda.time.DateTimeConstants.TUESDAY
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.WebSocket
import play.api.libs.json._
import _root_.actors.WebSocketActor
import play.api.Play.current
import scala.concurrent.Future
import lib.OvChipkaartClient
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Enumerator
import play.api.cache.Cache

object Application extends Controller {

  case class PdfRequest(username: String, password: String, period: String, transactions: List[String])
  
  object PdfRequest {
    val format = Json.format[PdfRequest]
  }
  
  def index = Action {
    Ok(views.html.index())
  }
  
  def ov = WebSocket.tryAcceptWithActor[JsValue, JsValue] { request =>
    Future.successful(Right(WebSocketActor.props _))
  }
  
  def pdf(uuid: String) = Action { request =>
    Cache.get(uuid) match {
      case Some((period: String, tempFile: File)) =>
        Ok.sendFile(tempFile, false, _ => period + ".pdf")
      case _ =>
        BadRequest("Unknown PDF")
    }
  }

  def process = Action(parse.multipartFormData) { request =>
    request.body.file("transacties").map { transacties =>
      import java.io.File
      val filename = transacties.filename
      val contentType = transacties.contentType
      val in = File.createTempFile("input", "temp")
      transacties.ref.moveTo(in, replace = true)

      val workbookIn = WorkbookFactory.create(in)
      val workbookOut = new XSSFWorkbook()

      val sheetNames = List("overzicht", "januari", "februari", "maart", "april", "mei", "juni", "juli", "augustus", "september", "oktober", "november", "december")
      for (maand <- sheetNames) {
        workbookOut.createSheet(maand);
      }

      val dataFormat = workbookOut.createDataFormat()
      val createHelper = workbookOut.getCreationHelper()
      val dateStyle = workbookOut.createCellStyle()
      dateStyle.setDataFormat(
        createHelper.createDataFormat().getFormat("dd/mm/yyyy"));
      val currencyStyle = workbookOut.createCellStyle()
      currencyStyle.setDataFormat(dataFormat.getFormat("€\\ #,##0.00;€\\ #,##0.00"))

      val sheetIn = workbookIn.getSheetAt(0)

      case class Record(date: DateTime, from: String, to: String, cost: Double) {
        val day = date.dayOfWeek().get()

        def defaultWorkDay: Boolean = day >= TUESDAY && day <= THURSDAY

        def hasDestination(dest: String) = (from contains dest) || (to contains dest)

        def valid: Boolean = defaultWorkDay
      }

      println(sheetIn.getLastRowNum())

      val records = for (r <- 1 to sheetIn.getLastRowNum() - 1) yield {
        val row = sheetIn.getRow(r)

        val date = new DateTime(row.getCell(0).getDateCellValue())

        Record(date, row.getCell(2).getStringCellValue(), row.getCell(4).getStringCellValue(), row.getCell(5).getNumericCellValue())
      }

      records.filterNot(_.valid).foreach(println _)

      val byMonth = records.filter(_.valid).sortBy(_.date.toDate()).groupBy(_.date.monthOfYear.get)

      for (month <- byMonth) {
        val (monthNr, records) = month
        val summary = workbookOut.getSheetAt(0).createRow(monthNr)
        summary.createCell(0).setCellValue(sheetNames(monthNr))
        val summarySum = summary.createCell(1)
        summarySum.setCellValue(records.map(_.cost).sum)
        summarySum.setCellStyle(currencyStyle)

        val monthSheet = workbookOut.getSheetAt(monthNr)

        for ((record, i) <- records.zipWithIndex) {
          val monthRow = monthSheet.createRow(i)
          val dateCell = monthRow.createCell(0)
          dateCell.setCellValue(record.date.toDate())
          dateCell.setCellStyle(dateStyle)
          monthRow.createCell(1).setCellValue(record.from)
          monthRow.createCell(2).setCellValue(record.to)
          val costCell = monthRow.createCell(3)
          costCell.setCellValue(record.cost)
          costCell.setCellStyle(currencyStyle)
        }

        val sumRow = monthSheet.createRow(records.size + 1)
        sumRow.createCell(2).setCellValue("Totaal")
        val sumCell = sumRow.createCell(3)
        sumCell.setCellFormula("SUM(D1:D" + records.size + ")")
        sumCell.setCellStyle(currencyStyle)
      }

      val temp = File.createTempFile("output", "temp")
      val out = new FileOutputStream(temp)
      workbookOut.write(out)
      out.close()

      Ok.sendFile(content = temp, fileName = _ => "overzicht.xlsx")
    }.getOrElse {
      Redirect(routes.Application.index).flashing(
        "error" -> "Missing file")
    }
  }

}