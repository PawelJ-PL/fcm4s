package com.github.pawelj_pl.fcm4s.messaging

import com.github.pawelj_pl.fcm4s.messaging.extrafields.{Android, Apns, Webpush}
import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec}
import io.circe.{Encoder, Json}
import io.circe.syntax._

sealed trait Message[A] {
  val name: Option[String]
  val android: Option[Android]
  val webpush: Option[Webpush]
  val apns: Option[Apns]
  val fcmOptions: Option[FcmOptions]
}

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

  private[messaging] def commonJsonFields[A](message: Message[A]): List[Option[(String, Json)]] = List(
    message.name.map(name => ("name", Json.fromString(name))),
    message.android.map(android => ("android", android.asJson.dropNullValues)),
    message.webpush.map(webpush => ("webpush", webpush.asJson.dropNullValues)),
    message.apns.map(apns => ("apns", apns.asJson.dropNullValues)),
    message.fcmOptions.map(fcmOpts => ("fcm_options", fcmOpts.asJson))
  )
}

final case class DataMessage[A: MessageDataEncoder](
  destination: Destination,
  data: A,
  name: Option[String] = None,
  android: Option[Android] = None,
  webpush: Option[Webpush] = None,
  apns: Option[Apns] = None,
  fcmOptions: Option[FcmOptions] = None)
    extends Message[A]

object DataMessage {
  implicit def encoder[A: MessageDataEncoder]: Encoder[DataMessage[A]] = new Encoder[DataMessage[A]] {
    override def apply(a: DataMessage[A]): Json = {
      val destinationJson = Message.destinationAsJsonEntry(a.destination)
      val messageJsonElements = Message
        .commonJsonFields(a)
        .:+(Some(("data", MessageDataEncoder[A].encode(a.data).asJson)))
        .:+(Some(destinationJson))
        .flatten
      Json.obj(
        ("message",
         Json.obj(
           messageJsonElements: _*
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
  data: Option[A] = None,
  name: Option[String] = None,
  android: Option[Android] = None,
  webpush: Option[Webpush] = None,
  apns: Option[Apns] = None,
  fcmOptions: Option[FcmOptions] = None)
    extends Message[A]

object NotificationMessage {
  implicit def encoder[A: MessageDataEncoder]: Encoder[NotificationMessage[A]] = new Encoder[NotificationMessage[A]] {
    override def apply(a: NotificationMessage[A]): Json = {
      val destinationJson = Message.destinationAsJsonEntry(a.destination)
      val notificationValuesJson = List(
        a.title.map(title => ("title", Json.fromString(title))),
        a.body.map(body => ("body", Json.fromString(body))),
        a.image.map(image => ("image", Json.fromString(image)))
      ).flatten
      val messageJsonElements = Message
        .commonJsonFields(a)
        .flatten
        .:+(("data", a.data.map(data => MessageDataEncoder[A].encode(data).asJson).getOrElse(Json.Null)))
        .:+(("notification", Json.obj(notificationValuesJson: _*)))
        .:+(destinationJson)
      Json.obj(
        ("message", Json.obj(messageJsonElements: _*))
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

@ConfiguredJsonCodec(encodeOnly = true)
final case class FcmOptions(analyticsLabel: Option[String])

object FcmOptions {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames
}
