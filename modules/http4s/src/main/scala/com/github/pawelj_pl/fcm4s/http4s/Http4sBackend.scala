package com.github.pawelj_pl.fcm4s.http4s

import cats.data.Chain
import cats.effect.{ConcurrentEffect, Sync}
import cats.syntax.either._
import cats.syntax.functor._
import com.github.pawelj_pl.fcm4s.http.HttpBackend
import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import org.http4s.{EntityDecoder, Header, Headers, Method, Request, Uri, UrlForm}
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.circe._

import scala.concurrent.ExecutionContext

class Http4sBackend[F[_]: Sync](client: Client[F]) extends HttpBackend[F] {
  override def sendPostForm[A: Decoder](uri: Uri, formData: Map[String, String]): F[A] = {
    implicit val respDecoder: EntityDecoder[F, A] = jsonOf[F, A]
    val form: Map[String, Chain[String]] = formData.map { case (key, value) => (key, Chain(value)) }
    val request = Request[F](method = Method.POST, uri = uri).withEntity(UrlForm(form))
    client.expect[A](request)
  }

  override def sendPost[A: Encoder, B: Decoder](uri: Uri, payload: A, bearerToken: String): F[Either[(Int, String), B]] = {
    implicit val respDecoder: EntityDecoder[F, B] = jsonOf[F, B]
    val authorizationHeader = Header("Authorization", s"Bearer $bearerToken")
    val request = Request[F](method = Method.POST, uri = uri, headers = Headers.of(authorizationHeader)).withEntity(payload.asJson)
    client.fetch(request)(resp => {
      if (resp.status.isSuccess) {
        resp.as[B].map(body => body.asRight[(Int, String)])
      } else {
        resp.as[String].map(body => (resp.status.code, body).asLeft[B])
      }
    })
  }
}

object Http4sBackend {
  def apply[F[_]: Sync](client: Client[F]): Http4sBackend[F] = new Http4sBackend(client)
  def create[F[_]: ConcurrentEffect](executionContext: ExecutionContext): F[Http4sBackend[F]] =
    BlazeClientBuilder[F](executionContext).resource.use(client => Sync[F].delay(Http4sBackend(client)))
}
