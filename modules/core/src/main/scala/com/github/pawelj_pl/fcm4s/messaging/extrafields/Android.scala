package com.github.pawelj_pl.fcm4s.messaging.extrafields

import java.time.Instant

import com.github.pawelj_pl.fcm4s.messaging.extrafields.EncodingImplicits._
import com.github.pawelj_pl.fcm4s.messaging.{FcmOptions, MessageData}
import io.circe.{Encoder, derivation}
import io.circe.generic.semiauto.deriveEncoder
import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec}

import scala.concurrent.duration.Duration

@ConfiguredJsonCodec(encodeOnly = true)
final case class Android(
  collapseKey: Option[String] = None,
  priority: Option[MessagePriority] = None,
  ttl: Option[Duration] = None,
  restrictedPackageName: Option[String] = None,
  data: Option[MessageData] = None,
  notification: Option[AndroidNotification] = None,
  fcmOptions: Option[FcmOptions] = None)

object Android {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames
}

sealed trait MessagePriority extends Product with Serializable

object MessagePriority {
  implicit val encoder: Encoder[MessagePriority] = Encoder.encodeString.contramap(_.toString.toLowerCase)

  case object Normal extends MessagePriority
  case object High extends MessagePriority
}

final case class AndroidNotification(
  title: Option[String] = None,
  body: Option[String] = None,
  icon: Option[String] = None,
  color: Option[String] = None,
  sound: Option[String] = None,
  tag: Option[String] = None,
  clickAction: Option[String] = None,
  bodyLocKey: Option[String] = None,
  bodyLocArgs: Option[List[String]] = None,
  titleLocKey: Option[String] = None,
  titleLocArgs: Option[List[String]] = None,
  channelId: Option[String] = None,
  ticker: Option[String] = None,
  sticky: Option[Boolean] = None,
  eventTime: Option[Instant] = None,
  localOnly: Option[Boolean] = None,
  notificationPriority: Option[AndroidNotificationPriority] = None,
  defaultSound: Option[Boolean] = None,
  defaultVibrateTimings: Option[Boolean] = None,
  defaultLightSettings: Option[Boolean] = None,
  vibrateTimings: Option[List[Duration]] = None,
  visibility: Option[AndroidNotificationVisibility] = None,
  notificationCount: Option[Int] = None,
  lightSettings: Option[LightSettings] = None,
  image: Option[String] = None)

object AndroidNotification {
  implicit val snakeCaseEncoder: Encoder[AndroidNotification] =
    io.circe.derivation.deriveEncoder[AndroidNotification](derivation.renaming.snakeCase).mapJson(_.dropNullValues)
}

sealed trait AndroidNotificationPriority extends Product with Serializable

object AndroidNotificationPriority {
  case object PriorityUnspecified extends AndroidNotificationPriority
  case object PriorityMin extends AndroidNotificationPriority
  case object PriorityLow extends AndroidNotificationPriority
  case object PriorityDefault extends AndroidNotificationPriority
  case object PriorityHigh extends AndroidNotificationPriority
  case object PriorityMax extends AndroidNotificationPriority

  implicit val encoder: Encoder[AndroidNotificationPriority] =
    Encoder.encodeString.contramap(prio => derivation.renaming.snakeCase(prio.toString).toUpperCase)
}

sealed trait AndroidNotificationVisibility extends Product with Serializable

object AndroidNotificationVisibility {
  case object VisibilityUnspecified extends AndroidNotificationVisibility
  case object Private extends AndroidNotificationVisibility
  case object Public extends AndroidNotificationVisibility
  case object Secret extends AndroidNotificationVisibility

  implicit val encoder: Encoder[AndroidNotificationVisibility] =
    Encoder.encodeString.contramap(visibility => derivation.renaming.snakeCase(visibility.toString).toUpperCase)
}

final case class Color(red: Double, green: Double, blue: Double, alpha: Double)

object Color {
  implicit val encoder: Encoder[Color] = deriveEncoder[Color]
}

@ConfiguredJsonCodec(encodeOnly = true)
final case class LightSettings(color: Color, lightOnDuration: Duration, lightOffDuration: Duration)

object LightSettings {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames
}
