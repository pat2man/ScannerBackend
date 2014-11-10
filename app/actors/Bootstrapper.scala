package actors

import akka.actor.{Props, Actor}
import com.github.sstone.amqp.Amqp._
import com.github.sstone.amqp.{Amqp, Consumer, ConnectionOwner}
import com.rabbitmq.client.ConnectionFactory
import play.api.Logger
import play.api.libs.json.{Json, JsValue}
import play.libs.Akka

/**
 * Created by pat2man on 11/7/14.
 */

object Bootstrapper {
  def props(): Props = Props(classOf[Bootstrapper])
}

class Bootstrapper extends Actor {

  val consumer = {
    val connFactory = new ConnectionFactory()
    connFactory.setUri("amqp://guest:guest@localhost")
    val conn = context.actorOf(ConnectionOwner.props(connFactory))
    val consumer = ConnectionOwner.createChildActor(conn, Consumer.props(self, channelParams = None, autoack = false))
    Amqp.waitForConnection(Akka.system, consumer).await()
    consumer
  }

  override def preStart(): Unit = {
    consumer ! DeclareQueue(QueueParameters("all-mqtt-messages", passive = false, durable = false, exclusive = false, autodelete = true))
    consumer ! QueueBind("all-mqtt-messages", "amq.topic", "venues.*.devices.*")
    consumer ! AddQueue(QueueParameters(name = "all-mqtt-messages", passive = false))
  }

  def receive = {
    case Delivery(consumerTag, envelope, properties, body) => {
      val venueDevicePattern = """venues\.(.*)\.devices\.(.*)""".r
      envelope.getRoutingKey() match {
        case venueDevicePattern(venue, device) =>
          val action = new String(body)
          action match {
            case "FetchBarcodes" => {
              val child = context.actorOf(Retriever.props(consumer, s"venues.$venue.barcodes", s"devices.$device"))
            }
          }
        case _ =>
          Logger.info("Couldn't find venue ID or device ID in routing key")
      }
    }
    case message => {
      Logger.info("Received unknown message: " + message)
    }
  }
}
