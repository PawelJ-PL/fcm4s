package com.github.pawelj_pl.fcm4s.messaging

import io.circe.{Encoder, Json}
import io.circe.syntax._

sealed trait Message

object Message {
  implicit val encoder: Encoder[Message] = Encoder.instance {
    case m: DataMessage         => m.asJson
    case m: NotificationMessage => m.asJson
  }

  private[messaging] def destinationAsJsonEntry(destination: Destination): (String, Json) = destination match {
    case Destination.Token(t)     => ("token", Json.fromString(t))
    case Destination.Topic(t)     => ("topic", Json.fromString(t))
    case Destination.Condition(c) => ("condition", Json.fromString(c))
  }
}

final case class DataMessage(destination: Destination, data: MessageData) extends Message

object DataMessage {
  implicit val encoder: Encoder[DataMessage] = new Encoder[DataMessage] {
    override def apply(a: DataMessage): Json = {
      val destinationJson = Message.destinationAsJsonEntry(a.destination)
      Json.obj(
        ("message",
         Json.obj(
           destinationJson,
           ("data", a.data.asJson)
         ))
      )
    }
  }
}

final case class NotificationMessage(
  destination: Destination,
  title: Option[String] = None,
  body: Option[String] = None,
  image: Option[String] = None,
  data: Option[MessageData] = None)
    extends Message

object NotificationMessage {
  implicit val encoder: Encoder[NotificationMessage] = new Encoder[NotificationMessage] {
    override def apply(a: NotificationMessage): Json = {
      val destinationJson = Message.destinationAsJsonEntry(a.destination)
      Json.obj(
        ("message",
         Json.obj(
           destinationJson,
           ("notification",
            Json.obj(
              ("title", a.title.map(Json.fromString).getOrElse(Json.Null)),
              ("body", a.body.map(Json.fromString).getOrElse(Json.Null)),
              ("image", a.image.map(Json.fromString).getOrElse(Json.Null))
            )),
           ("data", a.data.map(data => data.asJson).getOrElse(Json.Null))
         ))
      )
    }
  }
}

sealed trait Destination extends Product with Serializable

object Destination {
  final case class Token(token: String) extends Destination
  final case class Topic(topic: String) extends Destination
  final case class Condition(condition: String) extends Destination
}
