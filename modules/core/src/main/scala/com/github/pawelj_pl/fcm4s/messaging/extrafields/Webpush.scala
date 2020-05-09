package com.github.pawelj_pl.fcm4s.messaging.extrafields

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec}

@ConfiguredJsonCodec(encodeOnly = true)
final case class Webpush(
  headers: Option[Map[String, String]] = None,
  data: Option[Map[String, String]] = None,
  notification: Option[WebpushNotification] = None,
  fcmOptions: Option[WebpushFcmOptions] = None)

object Webpush {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames
}

final case class WebpushNotification(
  actions: Option[Map[String, String]] = None,
  badge: Option[String] = None,
  body: Option[String] = None,
  data: Option[Map[String, String]] = None,
  dir: Option[WebpushNotificationDirection] = None,
  lang: Option[String] = None,
  tag: Option[String] = None,
  icon: Option[String] = None,
  image: Option[String] = None,
  renotify: Option[Boolean] = None,
  requireInteraction: Option[Boolean] = None,
  silent: Option[Boolean] = None,
  timestamp: Option[Long] = None,
  title: Option[String] = None,
  vibrate: Option[List[Int]] = None)

object WebpushNotification {
  implicit val encoder: Encoder[WebpushNotification] = deriveEncoder[WebpushNotification].mapJson(_.dropNullValues)
}

sealed trait WebpushNotificationDirection extends Product with Serializable

object WebpushNotificationDirection {
  case object Auto extends WebpushNotificationDirection
  case object Ltr extends WebpushNotificationDirection
  case object Rtl extends WebpushNotificationDirection

  implicit val encoder: Encoder[WebpushNotificationDirection] = Encoder.encodeString.contramap(_.toString.toLowerCase)
}

@ConfiguredJsonCodec(encodeOnly = true)
final case class WebpushFcmOptions(link: Option[String] = None, analyticsLabel: Option[String] = None)

object WebpushFcmOptions {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames
}
