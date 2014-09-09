package lib

import scala.concurrent.{ Future, ExecutionContext }
import scala.collection.JavaConversions._
import com.gargoylesoftware.htmlunit._
import com.gargoylesoftware.htmlunit.html._

object OvChipkaartClient {
  object urls {
    val root = "https://www.ov-chipkaart.nl/"
    val login = root + "login/"
    val transactions = root + "mijnovchipkaart/reizenentransacties/mijnreizenentransacties/"
  }
  case class Transaction(period: String, date: String, time: String, description: String, in: String, out: String, price: Double, name: String)

  def withClient[T](context: ExecutionContext)(fn: OvChipkaartClient => Future[T]): (String, String) => Future[T] = (username: String, password: String) => {
    implicit val ctxt = context
    val client = new OvChipkaartClient(username, password)
    val result = fn(client)
    result.andThen {
      case _ => client.close
    }
    result
  }

  def checkLogin(implicit context: ExecutionContext) = withClient(context) { client =>
    client.checkLogin
  }

  def listPeriods(implicit context: ExecutionContext) = withClient(context) { client =>
    client.listPeriods
  }

  def listTransactions(period: String, callback: Transaction => Unit)(implicit context: ExecutionContext) = withClient(context) { client =>
    client.listTransactions(period, callback)
  }

  def pdf(period: String, transactions: Set[String])(implicit context: ExecutionContext) = withClient(context) { client =>
    client.pdf(period, transactions)
  }
}

class OvChipkaartClient(val username: String, val password: String)(implicit context: ExecutionContext) {

  import OvChipkaartClient._

  val driver = new WebClient()

  object pages {
    def login: HtmlPage = driver.getPage(urls.login)
    def transactions: HtmlPage = driver.getPage(urls.transactions)
  }

  def checkLogin: Future[Boolean] = Future {
    val p = pages.login
    val f = p.getForms()(1)
    f.getInputByName[HtmlTextInput]("gebruikersnaam:input").setText(username)
    f.getInputByName[HtmlPasswordInput]("wachtwoord:input").setText(password)

    val response: HtmlPage = f.getButtonByName("inloggen").click()
    !response.asText().contains("Gebruikersnaam en/of wachtwoord incorrect")
  }

  def whenLoggedIn[T](fn: => T): Future[Either[String, T]] = checkLogin.map { loggedIn =>
    loggedIn match {
      case true =>
        Right(fn)
      case false =>
        Left("Invalid login")
    }
  }

  def pdf(period: String, transactions: Set[String]) = whenLoggedIn {
    val p = pages.transactions
    val f = p.getForms()(2)
    f.getSelectByName("transactieTypes").getOptionByText("Reistransacties").setSelected(true)
    f.getSelectByName("periodes").getOptionByText(period).setSelected(true)
    val response: HtmlPage = f.getButtonByName("submitzoekopdracht").click()

    val page = checkTransactions(period, response, transactions)

    val pdfPage: TextPage = page.getFirstByXPath[HtmlAnchor]("//li[@class = 'pdf']/a").click()
    pdfPage.getWebResponse().getContentAsStream()
  }

  private def checkTransactions(period: String, page: HtmlPage, transactions: Set[String]): HtmlPage = {
    val table: HtmlTable = page.getFirstByXPath("//table[contains(@class, 'transacties')]")
    val checkboxes = table.getRows().drop(1).map(row =>
      row.getCell(3).getHtmlElementsByTagName[HtmlCheckBoxInput]("input")(0))
    val mustBeChanged = checkboxes.find { checkbox =>
      val name = checkbox.getAttributes().getNamedItem("name").getTextContent()
      val newState = transactions.contains(name)
      checkbox.isChecked() != newState
    }

    try {
      val topRow = table.getRow(1).asXml()

      val nextPage: HtmlPage = mustBeChanged match {
        // change the checkbox
        case Some(checkbox) =>
          println(s"${if (checkbox.isChecked()) "Unchecking" else "Checking"} ${checkbox.getAttributes().getNamedItem("name").getTextContent()}")
          checkbox.click()

        // nothing left to change, so move to next page
        case None =>
          println("Next page")
          page.getAnchorByText("volgende").click()
      }

      val start = System.currentTimeMillis()
      while (nextPage.getFirstByXPath[HtmlTable]("//table[contains(@class, 'transacties')]").getRow(1).asXml() == topRow && System.currentTimeMillis() - start < 5000) {
        println("Waiting for page change...")
        Thread.sleep(100)
      }
      checkTransactions(period, nextPage, transactions)
    } catch {
      case e: Exception =>
        println(e.getMessage())
      // no more transactions
        page
    }
  }

  def listPeriods = whenLoggedIn {
    val p = pages.transactions
    val f = p.getForms()(2)

    val options: List[String] = f.getSelectByName("periodes").getOptions().map(p => p.getText()).toList
    options
  }

  def listTransactions(period: String, callback: Transaction => Unit = _ => Unit): Future[Either[String, List[Transaction]]] = whenLoggedIn {
    val p = pages.transactions
    val f = p.getForms()(2)
    f.getSelectByName("transactieTypes").getOptionByText("Reistransacties").setSelected(true)
    f.getSelectByName("periodes").getOptionByText(period).setSelected(true)
    val response: HtmlPage = f.getButtonByName("submitzoekopdracht").click()

    listTransactions(period, response, callback)
  }

  private def listTransactions(period: String, page: HtmlPage, callback: Transaction => Unit): List[Transaction] = {
    def reorderDate(date: String): String = {
      val day = date.take(2)
      val month = date.drop(3).take(2)
      val year = date.takeRight(4)
      s"$year-$month-$day"
    }
    val table: HtmlTable = page.getFirstByXPath("//table[contains(@class, 'transacties')]")
    try {
      val transactions = table.getRows().drop(1).flatMap { row =>
        val description = row.getCell(1).asText()
        if (description.startsWith("Check-uit")) {
          val re = """(?s)Check-uit (\d\d:\d\d) (.*) bij .*Check-in (.*) bij .*Ritprijs € (\d+,\d+)""".r
          re.findFirstIn(description) match {
            case Some(re(time, out, in, price)) =>
              val name = row.getCell(3).getFirstChild().getAttributes().getNamedItem("name").getTextContent()
              println(name)
              val tx = Transaction(period, reorderDate(row.getCell(0).asText()), time, description, in, out, price.replace(",", ".").toDouble, name)
              callback(tx)
              Some(tx)
            case _ => None
          }
        } else {
          None
        }
      }.toList
      val topRow = table.getRow(1).asXml()
      val nextPage: HtmlPage = page.getAnchorByText("volgende").click()
      while (nextPage.getFirstByXPath[HtmlTable]("//table[contains(@class, 'transacties')]").getRow(1).asXml() == topRow) {
        println("Waiting for page change...")
        Thread.sleep(100)
      }
      transactions ++ listTransactions(period, nextPage, callback)
    } catch {
      case e: Exception =>
        println(e.getMessage())
        // no more transactions
        Nil
    }
  }

  def close = driver.closeAllWindows()
}
