package actors

import play.api.libs.concurrent.Akka
import akka.actor._
import play.api.libs.json._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.Enumeratee
import play.api.libs.iteratee.Iteratee
import play.api.Play.current

object WebSocketActor {

  def props(out: ActorRef) = {
    Props(new WebSocketActor(out))
  }

}

class WebSocketActor(out: ActorRef) extends Actor {

  implicit val timeout = Timeout(30 seconds)
  
  val ov = Akka.system.actorOf(Props[OvChipkaartActor])

  import OvChipkaartActor._
  import lib.OvChipkaartClient.Transaction
  
  def receive = {
    case JsObject(Seq(("username", JsString(username)), ("password", JsString(password)))) =>
      ov ! Login(username, password)
      ov ! ListPeriods
      
    case JsString(period) =>
      ov ! SelectPeriod(period)
    
    case Periods(periods) =>
      out ! Json.obj("periods" -> periods.map(_.name))
      
    case Transaction(period, date, time, desc, locIn, locOut, price) =>
      out ! Json.obj("transaction" -> Json.obj("period" -> period, "date" -> date, "time" -> time, "description" -> desc, "in" -> locIn, "out" -> locOut, "price" -> price))
    
    case Finished(period) =>
      out ! Json.obj("finished" -> period)
     
    case Error(error) =>
      out ! Json.obj("error" -> error)
  }
  
  override def postStop() = {
    ov ! PoisonPill
  }
}