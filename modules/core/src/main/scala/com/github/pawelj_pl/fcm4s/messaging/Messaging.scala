package com.github.pawelj_pl.fcm4s.messaging

import cats.instances.list._
import cats.Monad
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import com.github.pawelj_pl.fcm4s.auth.AccessTokenAuth
import com.github.pawelj_pl.fcm4s.auth.config.CredentialsConfig
import com.github.pawelj_pl.fcm4s.http.HttpBackend
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.deriveDecoder
import org.http4s.implicits._


trait Messaging[F[_]] {
  def send[A: Encoder](message: Message[A]): F[Either[ErrorResponse, String]]
  def sendMany[A: Encoder](messages: List[Message[A]]): F[List[Either[ErrorResponse, String]]]
  def sendMany[A: Encoder](messages: Message[A]*): F[List[Either[ErrorResponse, String]]]
}

object Messaging {
  def apply[F[_]](implicit ev: Messaging[F]): Messaging[F] = ev

  def create[F[_]: Monad: AccessTokenAuth: HttpBackend](config: CredentialsConfig): Messaging[F] = new Messaging[F] {
    private val SendMessageUri = uri"https://fcm.googleapis.com/v1/projects" / config.projectId / "messages:send"

    override def send[A: Encoder](message: Message[A]): F[Either[ErrorResponse, String]] = for {
      token <- AccessTokenAuth[F].token
      resp  <-HttpBackend[F].sendPost[Message[A], SendMessageResponse](SendMessageUri, message, token)
    } yield resp.bimap({case (code, body) => ErrorResponse(code, body)}, _.name)

    override def sendMany[A: Encoder](messages: List[Message[A]]): F[List[Either[ErrorResponse, String]]] = messages
      .map(m => send(m))
      .sequence

    override def sendMany[A: Encoder](messages: Message[A]*): F[List[Either[ErrorResponse, String]]] = sendMany(messages.toList)
  }
}

case class SendMessageResponse(name: String)

object SendMessageResponse {
  implicit val decoder: Decoder[SendMessageResponse] = deriveDecoder[SendMessageResponse]
}

case class ErrorResponse(statusCode: Int, body: String)
