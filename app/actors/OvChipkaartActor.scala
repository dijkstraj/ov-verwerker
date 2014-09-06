package actors

import akka.actor._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import lib.OvChipkaartClient

object OvChipkaartActor {
  trait Request
  case class Login(username: String, password: String) extends Request
  case object ListPeriods extends Request
  case class SelectPeriod(period: String) extends Request

  trait Response
  case class Error(error: String) extends Response
  case class Period(name: String) extends Response
  case class Periods(periods: List[Period]) extends Response
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
      OvChipkaartClient.listTransactions(period, originalSender ! _)(defaultContext)(username, password)

    case _ =>
      println("Unknown message")
  }

}