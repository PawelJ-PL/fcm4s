package com.github.pawelj_pl.fcm4s.messaging

import cats.instances.list._
import cats.Monad
import cats.effect.{Clock, IO}
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import com.github.pawelj_pl.fcm4s.Common.ErrorResponse
import com.github.pawelj_pl.fcm4s.auth.AccessTokenAuth
import com.github.pawelj_pl.fcm4s.auth.config.CredentialsConfig
import com.github.pawelj_pl.fcm4s.http.HttpBackend
import com.github.pawelj_pl.fcm4s.utils.TimeProvider
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import org.http4s.Uri
import scalacache.Mode

trait Messaging[F[_]] {
  def send(message: Message): F[Either[ErrorResponse, String]]
  def sendMany(messages: List[Message]): F[List[Either[ErrorResponse, String]]]
  def sendMany(messages: Message*): F[List[Either[ErrorResponse, String]]]
}

object Messaging {
  def apply[F[_]](implicit ev: Messaging[F]): Messaging[F] = ev

  def create[F[_]: Monad: AccessTokenAuth: HttpBackend](config: CredentialsConfig): Messaging[F] = new Messaging[F] {
    private val SendMessageUri = Uri.uri("https://fcm.googleapis.com/v1/projects") / config.projectId / "messages:send"

    private def send(message: Message, authToken: String): F[Either[ErrorResponse, String]] =
      HttpBackend[F]
        .sendPost[Message, SendMessageResponse](SendMessageUri, message, authToken)
        .map(_.bimap({ case (code, body) => ErrorResponse(code, body) }, _.name))

    override def send(message: Message): F[Either[ErrorResponse, String]] =
      for {
        token  <- AccessTokenAuth[F].token
        result <- send(message, token)
      } yield result

    override def sendMany(messages: List[Message]): F[List[Either[ErrorResponse, String]]] =
      for {
        token  <- AccessTokenAuth[F].token
        result <- messages.map(m => send(m, token)).sequence
      } yield result

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
    import cats.implicits._
    val config: IO[CredentialsConfig] =
      CredentialsConfig
        .fromFile[IO](credentialsConfigPath)
        .map(_.leftMap(e => new RuntimeException(e.toString))
          .leftWiden[Throwable])
        .rethrow
    config.map(defaultIoMessaging)
  }
}

case class SendMessageResponse(name: String)

object SendMessageResponse {
  implicit val decoder: Decoder[SendMessageResponse] = deriveDecoder[SendMessageResponse]
}
