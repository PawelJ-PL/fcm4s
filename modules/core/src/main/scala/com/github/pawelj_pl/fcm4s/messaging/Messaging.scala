package com.github.pawelj_pl.fcm4s.messaging

import cats.instances.list._
import cats.Monad
import cats.effect.{Clock, IO}
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.monadError._
import cats.syntax.traverse._
import com.github.pawelj_pl.fcm4s.auth.AccessTokenAuth
import com.github.pawelj_pl.fcm4s.auth.config.CredentialsConfig
import com.github.pawelj_pl.fcm4s.http.HttpBackend
import com.github.pawelj_pl.fcm4s.utils.TimeProvider
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import org.http4s.implicits._
import scalacache.Mode

trait Messaging[F[_]] {
  def send(message: Message): F[Either[ErrorResponse, String]]
  def sendMany(messages: List[Message]): F[List[Either[ErrorResponse, String]]]
  def sendMany(messages: Message*): F[List[Either[ErrorResponse, String]]]
}

object Messaging {
  def apply[F[_]](implicit ev: Messaging[F]): Messaging[F] = ev

  def create[F[_]: Monad: AccessTokenAuth: HttpBackend](config: CredentialsConfig): Messaging[F] = new Messaging[F] {
    private val SendMessageUri = uri"https://fcm.googleapis.com/v1/projects" / config.projectId / "messages:send"

    override def send(message: Message): F[Either[ErrorResponse, String]] = for {
      token <- AccessTokenAuth[F].token
      resp  <-HttpBackend[F].sendPost[Message, SendMessageResponse](SendMessageUri, message, token)
    } yield resp.bimap({case (code, body) => ErrorResponse(code, body)}, _.name)

    override def sendMany(messages: List[Message]): F[List[Either[ErrorResponse, String]]] = messages
      .map(m => send(m))
      .sequence

    override def sendMany(messages: Message*): F[List[Either[ErrorResponse, String]]] = sendMany(messages.toList)
  }

  def defaultIoMessaging(credentialsConfig: CredentialsConfig)(implicit httpBackend: HttpBackend[IO]): Messaging[IO] = {
    implicit val clockIo: Clock[IO] = Clock.create[IO]
    implicit val timeProviderIo: TimeProvider[IO] = TimeProvider.instance[IO]
    implicit val cacheMode: Mode[IO] = scalacache.CatsEffect.modes.async
    implicit val authService: AccessTokenAuth[IO] = AccessTokenAuth.withCache[IO](credentialsConfig)
    Messaging.create[IO](credentialsConfig)
  }

  def defaultIoMessaging(credentialsConfigPath: String)(implicit httpBackend: HttpBackend[IO]): IO[Messaging[IO]] = {
    val config: IO[CredentialsConfig] = CredentialsConfig.fromFile[IO](credentialsConfigPath).map(_.leftMap(e => new RuntimeException(e.toString))).rethrow
    config.map(defaultIoMessaging)
  }
}

case class SendMessageResponse(name: String)

object SendMessageResponse {
  implicit val decoder: Decoder[SendMessageResponse] = deriveDecoder[SendMessageResponse]
}

case class ErrorResponse(statusCode: Int, body: String)
