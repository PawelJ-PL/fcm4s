package com.github.pawelj_pl.fcm4s.auth.config

import cats.data.EitherT
import cats.effect.{Resource, Sync}
import cats.syntax.applicativeError._
import cats.syntax.bifunctor._
import cats.syntax.either._
import io.circe.Error
import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec}
import io.circe.parser.decode
import org.http4s.circe._
import org.http4s.Uri

import scala.io.{BufferedSource, Source}
import scala.util.control.NonFatal

@ConfiguredJsonCodec
case class CredentialsConfig(clientId: String, clientEmail: String, tokenUri: Uri, privateKey: String, privateKeyId: String)

object CredentialsConfig {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames

  def fromFile[F[_]: Sync](filename: String): F[Either[ConfigError, CredentialsConfig]] = (for {
    content <- EitherT(readFileContent[F](filename))
    parsed  <- EitherT.fromEither(decode[CredentialsConfig](content)).leftMap(ConfigError.ConfigParsingError).leftWiden[ConfigError]
  } yield parsed).value

  private def readFileContent[F[_]: Sync](filename: String): F[Either[ConfigError, String]] = Resource
    .fromAutoCloseable[F, BufferedSource](Sync[F].delay(Source.fromFile(filename)))
    .map(_.mkString)
    .use(content => Sync[F].delay(content.asRight[ConfigError]))
    .recover{case NonFatal(e) => ConfigError.UnableToReadConfigFile(e).asLeft[String]}
}

sealed trait ConfigError extends Product with Serializable

object ConfigError {
  case class UnableToReadConfigFile(error: Throwable) extends ConfigError
  case class ConfigParsingError(error: Error) extends ConfigError
}