package com.github.pawelj_pl.fcm4s

import io.circe.Error

sealed trait Fcm4sError extends Product with Serializable

sealed trait ConfigError extends Fcm4sError

object ConfigError {
  final case class UnableToReadConfigFile(error: Throwable) extends ConfigError
  final case class ConfigParsingError(error: Error) extends ConfigError
}

sealed trait MessagingError extends Fcm4sError

object Common {
  final case class ErrorResponse(statusCode: Int, body: String) extends MessagingError
}