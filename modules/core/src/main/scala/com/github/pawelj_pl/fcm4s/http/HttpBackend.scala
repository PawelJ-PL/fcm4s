package com.github.pawelj_pl.fcm4s.http

import io.circe.Decoder
import org.http4s.Uri

trait HttpBackend[F[_]] {
  def sendPostForm[A: Decoder](uri: Uri, formData: Map[String, String]): F[A]
}

object HttpBackend {
  def apply[F[_]](implicit ev: HttpBackend[F]): HttpBackend[F] = ev
}