package com.github.pawelj_pl.fcm4s.auth

import java.time.temporal.ChronoUnit
import java.util.Base64

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.github.pawelj_pl.fcm4s.auth.config.CredentialsConfig
import com.github.pawelj_pl.fcm4s.http.HttpBackend
import com.github.pawelj_pl.fcm4s.utils.TimeProvider
import io.circe.syntax._
import scalacache.caffeine.CaffeineCache
import scalacache.{Mode, get, put}
import tsec.jws.signature.{JWSSignedHeader, JWTSig}
import tsec.jwt.{JWTClaims, JWTSingleAudience}
import tsec.signature.jca.SHA256withRSA

import scala.concurrent.duration.{Duration, SECONDS}

trait AccessTokenAuth[F[_]] {
  def token: F[String]
  def refresh: F[AccessTokenResponse]
}

object AccessTokenAuth {
  def apply[F[_]](implicit ev: AccessTokenAuth[F]): AccessTokenAuth[F] = ev

  def instance[F[_]: Sync: HttpBackend: TimeProvider](config: CredentialsConfig): AccessTokenAuth[F] = new AccessTokenAuth[F] {
    private val grantType = "urn:ietf:params:oauth:grant-type:jwt-bearer"
    private val scopes = List(
      "https://www.googleapis.com/auth/firebase.messaging",
      "https://www.googleapis.com/auth/cloud-platform"
    )

    override def token: F[String] = refresh.map(_.accessToken)

    override def refresh: F[AccessTokenResponse] = {
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
        now <- TimeProvider[F].instant
        claims = JWTClaims(
          issuer = Some(config.clientEmail),
          issuedAt = Some(now),
          expiration = Some(now.plus(1, ChronoUnit.HOURS)),
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

  def withCache[F[_]: Sync: HttpBackend: TimeProvider: Mode](config: CredentialsConfig): AccessTokenAuth[F] = new AccessTokenAuth[F] {
    private def delegate: AccessTokenAuth[F] = AccessTokenAuth.instance(config)
    private val CacheKey = s"token-${config.clientId}-${config.privateKeyId}"
    implicit val tokenResponseCache: CaffeineCache[AccessTokenResponse] = CaffeineCache[AccessTokenResponse]

    override def token: F[String] = OptionT(get(CacheKey))
      .getOrElseF(refresh)
      .map(_.accessToken)

    override def refresh: F[AccessTokenResponse] = for {
      resp <- delegate.refresh
      _    <- put(CacheKey)(resp, Some(Duration.create(resp.expiresIn, SECONDS)))
    } yield resp
  }
}
