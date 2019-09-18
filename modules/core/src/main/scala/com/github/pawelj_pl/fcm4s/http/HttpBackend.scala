package com.github.pawelj_pl.fcm4s.http

import io.circe.{Decoder, Encoder}
import org.http4s.Uri

trait HttpBackend[F[_]] {
  def sendPostForm[A: Decoder](uri: Uri, formData: Map[String, String]): F[Either[(Int, String),A]]
  def sendPost[A: Encoder, B: Decoder](uri: Uri, payload: A, bearerToken: String): F[Either[(Int, String), B]]
}

object HttpBackend {
  def apply[F[_]](implicit ev: HttpBackend[F]): HttpBackend[F] = ev
}