package com.github.pawelj_pl.fcm4s.messaging

import io.circe.{Encoder, Json}
import io.circe.syntax._

sealed trait Message[A]

object Message {
  implicit def encoder[A: Encoder]: Encoder[Message[A]] = Encoder.instance{
    case m: DataMessage[A] => m.asJson
  }
}

case class DataMessage[A: Encoder](destination: Destination, data: A) extends Message[A]

object DataMessage {
  implicit def encoder[A: Encoder]: Encoder[DataMessage[A]] = new Encoder[DataMessage[A]] {
    override def apply(a: DataMessage[A]): Json = {
      val destinationJson = a.destination match {
        case Destination.Token(t)     => ("token", Json.fromString(t))
        case Destination.Topic(t)     => ("topic", Json.fromString(t))
        case Destination.Condition(c) => ("condition", Json.fromString(c))
      }
      Json.obj(
        ("message", Json.obj(
          destinationJson,
          ("data", a.data.asJson),
//          ("notification", Json.obj(
//            ("title", Json.fromString(a.title)),
//            ("body", a.data.asJson)
//          ))
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