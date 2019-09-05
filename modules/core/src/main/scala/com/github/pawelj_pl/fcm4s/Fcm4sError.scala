package com.github.pawelj_pl.fcm4s

import io.circe.Error

trait Fcm4sError extends Product with Serializable

sealed trait ConfigError extends Fcm4sError

object ConfigError {
  case class UnableToReadConfigFile(error: Throwable) extends ConfigError
  case class ConfigParsingError(error: Error) extends ConfigError
}