package actors

import akka.actor._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import lib.OvChipkaartClient
import java.io.File
import org.apache.commons.io.IOUtils
import org.apache.commons.io.FileUtils
import java.io.FileOutputStream
import java.util.UUID

object OvChipkaartActor {
  trait Request
  case class Login(username: String, password: String) extends Request
  case object ListPeriods extends Request
  case class SelectPeriod(period: String) extends Request
  case class CreatePdf(period: String, transactions: Seq[String])

  trait Response
  case class Error(error: String) extends Response
  case class Period(name: String) extends Response
  case class Periods(periods: List[Period]) extends Response
  case class Finished(period: String) extends Response
  case class Pdf(period: String, uuid: String) extends Response
}

class OvChipkaartActor extends Actor {

  import OvChipkaartActor._

  var username = ""
  var password = ""
  
  def receive = {
    case Login(username, password) =>
      this.username = username
      this.password = password
      val originalSender = sender
      OvChipkaartClient.checkLogin(defaultContext)(username, password).foreach { loggedIn =>
        originalSender ! loggedIn
      }

    case ListPeriods =>
      val originalSender = sender
      OvChipkaartClient.listPeriods(defaultContext)(username, password).foreach { errorOrOptions =>
        errorOrOptions.fold(
            error => originalSender ! Error(error),
            options => originalSender ! Periods(options.map(p => Period(p)).toList)
        )
      }

    case SelectPeriod(period) =>
      val originalSender = sender
      OvChipkaartClient.listTransactions(period, originalSender ! _)(defaultContext)(username, password).onSuccess {
        case _ => originalSender ! Finished(period)
        
      }

    case CreatePdf(period, transactions) =>
      val originalSender = sender
      OvChipkaartClient.pdf(period, transactions.toSet)(defaultContext)(username, password).foreach { errorOrStream =>
        errorOrStream match {
          case Left(error) =>
            originalSender ! Error(error)
          case Right(stream) =>
            import play.api.cache.Cache
            val tempFile = File.createTempFile("ovverwerker", "pdf")
            IOUtils.copy(stream, new FileOutputStream(tempFile))
            val uuid = UUID.randomUUID().toString()
            Cache.set(uuid, period -> tempFile)
            originalSender ! Pdf(period, uuid)
        }
      }
      
      
    case _ =>
      println("Unknown message")
  }

}