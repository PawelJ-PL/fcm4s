package com.github.pawelj_pl.fcm4s.auth

import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec}

@ConfiguredJsonCodec
case class AccessTokenResponse(accessToken: String, expiresIn: Int, tokenType: String)

object AccessTokenResponse {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames
}