package com.github.pawelj_pl.fcm4s.messaging

import io.circe.{Encoder, Json}
import io.circe.syntax._

sealed trait Message[A]

object Message {
  implicit def encoder[A: MessageDataEncoder]: Encoder[Message[A]] = Encoder.instance {
    case m: DataMessage[A]         => m.asJson
    case m: NotificationMessage[A] => m.asJson
  }

  private[messaging] def destinationAsJsonEntry(destination: Destination): (String, Json) = destination match {
    case Destination.Token(t)     => ("token", Json.fromString(t))
    case Destination.Topic(t)     => ("topic", Json.fromString(t))
    case Destination.Condition(c) => ("condition", Json.fromString(c))
  }
}

final case class DataMessage[A: MessageDataEncoder](destination: Destination, data: A) extends Message[A]

object DataMessage {
  implicit def encoder[A: MessageDataEncoder]: Encoder[DataMessage[A]] = new Encoder[DataMessage[A]] {
    override def apply(a: DataMessage[A]): Json = {
      val destinationJson = Message.destinationAsJsonEntry(a.destination)
      Json.obj(
        ("message",
         Json.obj(
           destinationJson,
           ("data", MessageDataEncoder[A].encode(a.data).asJson)
         ))
      )
    }
  }
}

final case class NotificationMessage[A: MessageDataEncoder](
  destination: Destination,
  title: Option[String] = None,
  body: Option[String] = None,
  image: Option[String] = None,
  data: Option[A] = None)
    extends Message[A]

object NotificationMessage {
  implicit def encoder[A: MessageDataEncoder]: Encoder[NotificationMessage[A]] = new Encoder[NotificationMessage[A]] {
    override def apply(a: NotificationMessage[A]): Json = {
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
           ("data", a.data.map(data => MessageDataEncoder[A].encode(data).asJson).getOrElse(Json.Null))
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
