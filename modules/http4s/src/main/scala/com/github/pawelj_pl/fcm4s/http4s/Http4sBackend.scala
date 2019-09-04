package com.github.pawelj_pl.fcm4s.http4s

import cats.data.Chain
import cats.effect.{ConcurrentEffect, Sync}
import com.github.pawelj_pl.fcm4s.http.HttpBackend
import io.circe.Decoder
import org.http4s.{EntityDecoder, Method, Request, Uri, UrlForm}
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.circe._

import scala.concurrent.ExecutionContext

class Http4sBackend[F[_]: Sync](client: Client[F]) extends HttpBackend[F] {
  override def sendPostForm[A: Decoder](uri: Uri, formData: Map[String, String]): F[A] = {
    implicit val respDecoder: EntityDecoder[F, A] = jsonOf[F, A]
    val form = formData.mapValues(Chain(_)).toMap
    val request = Request[F](method = Method.POST, uri = uri).withEntity(UrlForm(form))
    client.expect[A](request)
  }
}

object Http4sBackend {
  def apply[F[_]: Sync](client: Client[F]): Http4sBackend[F] = new Http4sBackend(client)
  def create[F[_]: ConcurrentEffect](executionContext: ExecutionContext): F[Http4sBackend[F]] =
    BlazeClientBuilder[F](executionContext).resource.use(client => Sync[F].delay(Http4sBackend(client)))
}
