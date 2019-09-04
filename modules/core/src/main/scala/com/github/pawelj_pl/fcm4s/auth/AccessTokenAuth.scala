package com.github.pawelj_pl.fcm4s.auth

import java.util.Base64

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.github.pawelj_pl.fcm4s.auth.config.CredentialsConfig
import com.github.pawelj_pl.fcm4s.http.HttpBackend
import io.circe.syntax._
import tsec.jws.signature.{JWSSignedHeader, JWTSig}
import tsec.jwt.{JWTClaims, JWTSingleAudience}
import tsec.signature.jca.SHA256withRSA

import scala.concurrent.duration._

trait AccessTokenAuth[F[_]] {
  def refresh(): F[AccessTokenResponse]
}

object AccessTokenAuth {
  def instance[F[_]: Sync: HttpBackend](config: CredentialsConfig): AccessTokenAuth[F] = new AccessTokenAuth[F] {
    private val grantType = "urn:ietf:params:oauth:grant-type:jwt-bearer"
    private val scopes = List(
      "https://www.googleapis.com/auth/firebase.messaging",
      "https://www.googleapis.com/auth/cloud-platform"
    )

    override def refresh(): F[AccessTokenResponse] = {
      for {
        jwt <- generateJwt
        formData = Map("grant_type" -> grantType, "assertion" -> jwt)
        resp <- HttpBackend[F].sendPostForm[AccessTokenResponse](config.tokenUri, formData)
      } yield resp
    }

    private def generateJwt: F[String] =
      for {
        privKey <- SHA256withRSA.buildPrivateKey[F](Base64.getDecoder.decode(formatPrivateKey(config.privateKey)))
        header = JWSSignedHeader[SHA256withRSA](kid = Some(config.privateKeyId))
        scope = scopes.mkString(" ").asJson
        claims <- JWTClaims.withDuration(
          issuer = Some(config.clientEmail),
          issuedAt = Some(Duration.Zero),
          expiration = Some(1.hour),
          customFields = Seq(("scope", scope)),
          audience = Some(JWTSingleAudience(config.tokenUri.renderString))
        )
        jwt <- JWTSig.signToString(header, claims, privKey)
      } yield jwt

    private def formatPrivateKey(key: String): String =
      key
        .replaceAll("-+(BEGIN|END) PRIVATE KEY-+", "")
        .replace("\n", "")
  }
}
