package com.github.pawelj_pl.fcm4s.messaging.extrafields

import java.text.{DecimalFormat, DecimalFormatSymbols}

import io.circe.Encoder

import scala.concurrent.duration.Duration

object EncodingImplicits {
  implicit val durationEncoder: Encoder[Duration] =
    Encoder.encodeString.contramap(duration => {
      val symbols = new DecimalFormatSymbols()
      symbols.setDecimalSeparator('.')
      val format = new DecimalFormat("0.#########", symbols)
      format.format(duration.toNanos / 1000000000.0) + "s"
    })
}
