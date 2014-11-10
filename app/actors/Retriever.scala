package actors

import akka.actor.{ActorRef, Props, Actor}
import com.github.sstone.amqp.Amqp.{Publish}
import com.github.sstone.amqp.{Amqp, Consumer, ConnectionOwner}
import com.rabbitmq.client.ConnectionFactory
import play.api.Logger
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.{JsObject}
import play.api.libs.json.Json
import play.libs.Akka
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.MongoDriver
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Created by pat2man on 11/7/14.
 */
object Retriever {
  def props(consumer: ActorRef, topic: String, responseTopic: String): Props = Props(classOf[Retriever], consumer, topic, responseTopic)
}

class Retriever(consumer: ActorRef, topic: String, responseTopic: String) extends Actor {
  val barcodeEventsCollection: JSONCollection = {
    val driver = new MongoDriver
    val connection = driver.connection(List("localhost"))
    val db = connection("scanner")
    db("barcode_events")
  }

  override def preStart(): Unit = {
    val query = Json.obj(
      "topic" -> topic
    )
    barcodeEventsCollection.find(query)
      .cursor[JsObject].
      enumerate().apply(Iteratee.foreach { barcodeEvent =>
      val message = barcodeEvent \ "message"
      consumer ! Publish("amq.topic", responseTopic, message.toString().getBytes)
    })
  }

  def receive = {
    case message => {
    }
  }
}
