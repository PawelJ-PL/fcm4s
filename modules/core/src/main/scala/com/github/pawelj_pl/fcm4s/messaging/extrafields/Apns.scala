package com.github.pawelj_pl.fcm4s.messaging.extrafields

import io.circe.{Encoder, Json}
import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec}
import io.circe.generic.semiauto.deriveEncoder
import io.circe.syntax._

@ConfiguredJsonCodec(encodeOnly = true)
final case class Apns(headers: Option[Map[String, String]] = None, payload: Option[Json] = None, fcmOptions: Option[ApnsFcmOptions] = None)

object Apns {
  def withCustomPayload[A: Encoder](
    headers: Option[Map[String, String]] = None,
    payload: A,
    fcmOptions: Option[ApnsFcmOptions] = None
  ): Apns =
    new Apns(headers = headers, payload = Some(payload.asJson), fcmOptions = fcmOptions)
  def withStandardPayload(
    headers: Option[Map[String, String]] = None,
    payload: StandardApnsPayload,
    fcmOptions: Option[ApnsFcmOptions] = None
  ): Apns = {
    val apsPayload = Json.obj(("aps", payload.asJson.deepDropNullValues))
    new Apns(headers = headers, payload = Some(apsPayload), fcmOptions = fcmOptions)
  }
  def withMixedPayload[A: Encoder](
    headers: Option[Map[String, String]] = None,
    standard: StandardApnsPayload,
    customPayload: A,
    fcmOptions: Option[ApnsFcmOptions] = None
  ): Apns = {
    val apsPayload = Json.obj(("aps", standard.asJson.deepDropNullValues))
    val mergedPayload = apsPayload.asJson.deepMerge(customPayload.asJson)
    new Apns(headers = headers, payload = Some(mergedPayload), fcmOptions = fcmOptions)
  }

  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames
}

@ConfiguredJsonCodec(encodeOnly = true)
final case class ApnsFcmOptions(analyticsLabel: Option[String] = None, image: Option[String] = None)

object ApnsFcmOptions {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames
}

@ConfiguredJsonCodec(encodeOnly = true)
final case class StandardApnsPayload(
  alert: Option[ApnsPayloadAlert] = None,
  badge: Option[Int] = None,
  sound: Option[ApnsPayloadSound] = None,
  threadId: Option[String] = None,
  category: Option[String] = None,
  contentAvailable: Option[Int] = None,
  mutableContent: Option[Int] = None,
  targetContentId: Option[String] = None)

object StandardApnsPayload {
  implicit val customConfig: Configuration = Configuration.default.withKebabCaseMemberNames
}

sealed trait ApnsPayloadAlert extends Product with Serializable

object ApnsPayloadAlert {
  final case class AlertString(value: String) extends ApnsPayloadAlert

  object AlertString {
    implicit val encoder: Encoder[AlertString] = Encoder.encodeString.contramap(_.value)
  }

  @ConfiguredJsonCodec(encodeOnly = true)
  final case class AlertDict(
    title: Option[String] = None,
    subtitle: Option[String] = None,
    body: Option[String] = None,
    launchImage: Option[String] = None,
    titleLocKey: Option[String] = None,
    titleLocArgs: Option[List[String]] = None,
    subtitleLocKey: Option[String] = None,
    subtitleLocArgs: Option[List[String]] = None,
    locKey: Option[String] = None,
    locArgs: Option[List[String]] = None)
      extends ApnsPayloadAlert

  object AlertDict {
    implicit val customConfig: Configuration = Configuration.default.withKebabCaseMemberNames
  }

  implicit val encoder: Encoder[ApnsPayloadAlert] = Encoder.instance {
    case alert: AlertString => alert.asJson
    case alert: AlertDict   => alert.asJson
  }
}

sealed trait ApnsPayloadSound extends Product with Serializable

object ApnsPayloadSound {
  case object Default extends ApnsPayloadSound
  final case class SoundString(value: String) extends ApnsPayloadSound
  object SoundString {
    implicit val encoder: Encoder[SoundString] = Encoder.encodeString.contramap(_.value)
  }
  final case class SoundDict(critical: Int, name: String, volume: Double) extends ApnsPayloadSound
  object SoundDict {
    implicit val encoder: Encoder[SoundDict] = deriveEncoder[SoundDict]
  }

  implicit val encoder: Encoder[ApnsPayloadSound] = Encoder.instance {
    case Default            => Json.fromString("default")
    case sound: SoundString => sound.asJson
    case sound: SoundDict   => sound.asJson
  }
}
