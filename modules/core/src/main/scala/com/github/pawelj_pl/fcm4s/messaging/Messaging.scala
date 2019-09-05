package com.github.pawelj_pl.fcm4s.messaging

import cats.Monad
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.github.pawelj_pl.fcm4s.auth.AccessTokenAuth
import com.github.pawelj_pl.fcm4s.auth.config.CredentialsConfig
import com.github.pawelj_pl.fcm4s.http.HttpBackend
import io.circe.Encoder
import org.http4s.implicits._


trait Messaging[F[_]] {
  def send[A: Encoder](message: Message[A]): F[String]
}

object Messaging {
  def apply[F[_]](implicit ev: Messaging[F]): Messaging[F] = ev

  def create[F[_]: Monad: AccessTokenAuth: HttpBackend](config: CredentialsConfig): Messaging[F] = new Messaging[F] {
    private val SendMessageUri = uri"https://fcm.googleapis.com/v1/projects" / config.projectId / "messages:send"

    override def send[A: Encoder](message: Message[A]): F[String] = for {
      token <- AccessTokenAuth[F].token
      resp  <-HttpBackend[F].sendPost[Message[A], String](SendMessageUri, message, token)
    } yield token
  }
}