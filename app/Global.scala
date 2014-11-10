import akka.actor.{Actor, Props}
import play.libs.Akka
import play.api._
import actors.{Bootstrapper, Writer}

/**
 * Created by pat2man on 10/24/14.
 */
object Global extends GlobalSettings {


  override def onStart(app: Application) {
    Akka.system.actorOf(Bootstrapper.props())
    Akka.system.actorOf(Writer.props())
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }

}