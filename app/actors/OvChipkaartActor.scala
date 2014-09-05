package actors

import akka.actor._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.collection.JavaConversions._
import com.gargoylesoftware.htmlunit._
import com.gargoylesoftware.htmlunit.html._

object OvChipkaartActor {
  trait Request
  case class Login(username: String, password: String) extends Request
  case object ListPeriods extends Request
  case class SelectPeriod(period: String) extends Request

  trait Response
  case class Error(error: String) extends Response
  case class Period(name: String) extends Response
  case class Periods(periods: List[Period]) extends Response
  case class Transaction(date: String, time: String, description: String, in: String, out: String, price: Double) extends Response
  

  implicit class PimpedDriver(driver: WebClient) extends WebClient {
    object pages {
      def login: HtmlPage = driver.getPage("https://www.ov-chipkaart.nl/login/")
      def transactions: HtmlPage = driver.getPage("https://www.ov-chipkaart.nl/mijnovchipkaart/reizenentransacties/mijnreizenentransacties/")
    }
    
    def listTransactions(page: HtmlPage, callback: Transaction => Unit = _ => Unit): List[Transaction] = {
      def reorderDate(date: String): String = {
        val day = date.take(2)
        val month = date.drop(3).take(2)
        val year = date.takeRight(4)
        s"$year-$month-$day"
      }
      
      Thread.sleep(1000)
      val table: HtmlTable = page.getFirstByXPath("//table[contains(@class, 'transacties')]")
      try {
        val transactions = table.getRows().drop(1).flatMap { row =>
          val description = row.getCell(1).asText()
          if (description.startsWith("Check-uit")) {
            val re = """(?s)Check-uit (\d\d:\d\d) (.*) bij .*Check-in (.*) bij .*Ritprijs € (\d+,\d+)""".r
            re.findFirstIn(description) match {
              case Some(re(time, in, out, price)) =>
                val name = row.getCell(3).getFirstChild().getAttributes().getNamedItem("name").getTextContent()
                println(name)
                val tx = Transaction(reorderDate(row.getCell(0).asText()), time, description, in, out, price.replace(",", ".").toDouble)
                callback(tx)
                Some(tx)
              case _ => None              
            }
          } else {
            None
          }
        }.toList
        transactions ++ listTransactions(page.getAnchorByText("volgende").click(), callback)
      } catch {
        case e: Exception =>
          println(e.getMessage())
          // no more transactions
          Nil
      }
    }
  }
}

class OvChipkaartActor extends Actor {

  import OvChipkaartActor._

  val driver = new WebClient()

  var loggedIn = false

  def CheckLogin(fn: => Unit) = if (loggedIn) {
    fn
  } else {
    sender ! Error("Not logged in")
  }

  def receive = {
    case Login(username, password) =>
      val p = driver.pages.login
      val f = p.getForms()(1)
      f.getInputByName[HtmlTextInput]("gebruikersnaam:input").setText(username)
      f.getInputByName[HtmlPasswordInput]("wachtwoord:input").setText(password)
      
      val response: HtmlPage = f.getButtonByName("inloggen").click()
      loggedIn = !response.asText().contains("Gebruikersnaam en/of wachtwoord incorrect")
      sender ! loggedIn

    case ListPeriods =>
      CheckLogin {
        val p = driver.pages.transactions
        val f = p.getForms()(2)
        
        val options = f.getSelectByName("periodes").getOptions().map(p => Period(p.getText()))
        sender ! Periods(options.toList)
      }

    case SelectPeriod(period) =>
      CheckLogin {
        val p = driver.pages.transactions
        val f = p.getForms()(2)
        f.getSelectByName("transactieTypes").getOptionByText("Reistransacties").setSelected(true)
        f.getSelectByName("periodes").getOptionByText(period).setSelected(true)
        val response: HtmlPage = f.getButtonByName("submitzoekopdracht").click()
        
        driver.listTransactions(response, sender ! _)
      }

    case _ =>
      println("Unknown message")
  }

  override def postStop {
    driver.closeAllWindows()
  }
}