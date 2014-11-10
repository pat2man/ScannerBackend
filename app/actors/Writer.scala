package actors

import akka.actor.{Props, Actor}
import com.github.sstone.amqp.Amqp._
import com.github.sstone.amqp.{Amqp, Consumer, ConnectionOwner}
import com.rabbitmq.client.ConnectionFactory
import play.api.Logger
import play.api.libs.json.{Json, JsValue}
import play.libs.Akka
import reactivemongo.api._
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Created by pat2man on 11/7/14.
 */

object Writer {
  def props(): Props = Props(classOf[Writer])
}

class Writer extends Actor {

  val barcodeEventsCollection: JSONCollection = {
    val driver = new MongoDriver
    val connection = driver.connection(List("localhost"))
    val db = connection("scanner")
    db("barcode_events")
  }

  override def preStart(): Unit = {
    val connFactory = new ConnectionFactory()
    connFactory.setUri("amqp://guest:guest@localhost")
    
    val conn = context.actorOf(ConnectionOwner.props(connFactory))

    val consumer = ConnectionOwner.createChildActor(conn, Consumer.props(self, channelParams = None, autoack = false))
    Amqp.waitForConnection(Akka.system, consumer).await()
    consumer ! DeclareQueue(QueueParameters("barcode-events", passive = false, durable = false, exclusive = false, autodelete = true))
    consumer ! QueueBind("barcode-events", "amq.topic", "venues.*.barcodes.#")
    consumer ! AddQueue(QueueParameters(name = "barcode-events", passive = false))

    Logger.info("Started writer AMQP connection")
  }

  def receive = {
    case Delivery(consumerTag, envelope, properties, body) => {
      val json: JsValue = Json.parse(body)
      barcodeEventsCollection.insert(Json.obj(
        "message" -> json,
        "topic" -> envelope.getRoutingKey
      )).onSuccess{
        case _ => {
          sender ! Ack(envelope.getDeliveryTag)
        }
      }
    }
    case _ =>
      Logger.info("Got a non delivery message")
  }
}
