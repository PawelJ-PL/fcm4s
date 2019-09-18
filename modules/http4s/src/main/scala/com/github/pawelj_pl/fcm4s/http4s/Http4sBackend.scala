package com.github.pawelj_pl.fcm4s.http4s

import cats.data.Chain
import cats.effect.{ConcurrentEffect, Resource, Sync}
import cats.syntax.either._
import cats.syntax.functor._
import com.github.pawelj_pl.fcm4s.http.HttpBackend
import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import org.http4s.{EntityDecoder, Header, Headers, Method, Request, Response, Uri, UrlForm}
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.circe._

import scala.concurrent.ExecutionContext

class Http4sBackend[F[_]: Sync](client: Client[F]) extends HttpBackend[F] {
  override def sendPostForm[A: Decoder](uri: Uri, formData: Map[String, String]): F[Either[(Int, String), A]] = {
    val form: Map[String, Chain[String]] = formData.map { case (key, value) => (key, Chain(value)) }
    val request = Request[F](method = Method.POST, uri = uri).withEntity(UrlForm(form))
    client.fetch(request)(handleResponse[A])
  }

  override def sendPost[A: Encoder, B: Decoder](uri: Uri, payload: A, bearerToken: String): F[Either[(Int, String), B]] = {
    val authorizationHeader = Header("Authorization", s"Bearer $bearerToken")
    val request = Request[F](method = Method.POST, uri = uri, headers = Headers.of(authorizationHeader)).withEntity(payload.asJson)
    client.fetch(request)(handleResponse[B])
  }

  private def handleResponse[A: Decoder](response: Response[F]): F[Either[(Int, String), A]] = if (response.status.isSuccess) {
    implicit val respDecoder: EntityDecoder[F, A] = jsonOf[F, A]
    response.as[A].map(body => body.asRight[(Int, String)])
  } else {
    response.as[String].map(body => (response.status.code, body).asLeft[A])
  }
}

object Http4sBackend {
  def apply[F[_]: Sync](client: Client[F]): Http4sBackend[F] = new Http4sBackend(client)
  def create[F[_]: ConcurrentEffect](executionContext: ExecutionContext): Resource[F, Http4sBackend[F]] =
    BlazeClientBuilder[F](executionContext).resource.map(client => Http4sBackend(client))
}
