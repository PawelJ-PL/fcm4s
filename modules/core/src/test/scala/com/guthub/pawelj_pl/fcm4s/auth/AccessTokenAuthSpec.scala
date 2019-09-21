package com.guthub.pawelj_pl.fcm4s.auth

import java.time.Instant

import cats.effect.IO
import io.circe.literal._
import com.github.pawelj_pl.fcm4s.auth.{AccessTokenAuth, AccessTokenResponse}
import com.github.pawelj_pl.fcm4s.auth.config.CredentialsConfig
import com.guthub.pawelj_pl.fcm4s.testhelpers.fakes.{HttpBackendFake, TimeProviderFake}
import io.circe.Json
import org.http4s.Uri
import org.scalatest.{Matchers, WordSpec}
import scalacache.Mode

class AccessTokenAuthSpec extends WordSpec with Matchers {
  final val ExampleRsaKey =
    "-----BEGIN PRIVATE KEY-----\nMIIBPAIBAAJBAL2JdwKahOm9IG7UBkPU99oEWV4S70K3BjbJ0AQLKpctYyho33lb\naKNhGWzlazrEiyKqSOU/xSwQrdd6AnFSe3kCAwEAAQJBALntfHpoW9PyvDsb8F1g\nMBaFR6l6B405f3YFePJOheQvZzqPopnQpYymwVK72HDccV30GUt0TbSWWilbMgJa\nawECIQDvVcp/k7Gw+l1QSwbi7cShcpyMeVVP6+s3OJ1PKWTQWQIhAMq8AmDc6t6o\nrt2AmtRWri9bBAr06jHOu/b6ZTXefKAhAiEAgMJM8RnKTQZE0X+rssZsNNduNXzJ\nUvf/UXQZ3Y7Nd/ECIQCjkLpuge5wxDGI/jhstp6EEG+bk2vb0YqvQegkZSOxYQIg\ncdSrzvFvbDPn4g4QNK/MQAcCPU65WbMVxHNrqc0fhlI=\n-----BEGIN PRIVATE KEY-----"
  final val ExampleCredentialsConfig =
    CredentialsConfig("client1", "some@example.org", Uri.uri("http://localhost/token"), ExampleRsaKey, "key1", "project1")
  final val Now = Instant.ofEpochSecond(1265239341)
  implicit val timeProvider: TimeProviderFake[IO] = new TimeProviderFake[IO](Now)

  def httpBackend(responses: Seq[Either[(Int, String), Json]]): HttpBackendFake[IO] =
    HttpBackendFake.instanceF[IO](HttpBackendFake.BackendState(responses = responses)).unsafeRunSync()

  "access token auth" should {
    "send token refresh request" in {
      implicit val backend: HttpBackendFake[IO] = httpBackend(
        Seq(
          Right(
            json"""{
                "access_token": "resultToken",
                "expires_in": 10,
                "token_type": "bearer"
              }"""
          )
        ))
      val service = AccessTokenAuth.instance[IO](ExampleCredentialsConfig)
      service.refresh.unsafeRunSync() shouldBe AccessTokenResponse("resultToken", 10, "bearer")
    }

    "send request once and use cached value" when  {
      "create cached instance and ask for token only" in {
        implicit val cacheMode: Mode[IO] = scalacache.CatsEffect.modes.async
        implicit val backend: HttpBackendFake[IO] = httpBackend(
          Seq(
            Right(
              json"""{
                "access_token": "resultToken",
                "expires_in": 3600,
                "token_type": "bearer"
              }"""
            )
          ))
        val service = AccessTokenAuth.withCache[IO](ExampleCredentialsConfig)
        val result1 = service.token.unsafeRunSync()
        val result2 = service.token.unsafeRunSync()
        result1 shouldBe "resultToken"
        result2 shouldBe "resultToken"
        backend.getCalls.unsafeRunSync().length shouldBe 1
      }
    }

    "send request multiple times" when {
      "non cached instance created" in {
        implicit val backend: HttpBackendFake[IO] = httpBackend(
          Seq(
            Right(
              json"""{
                "access_token": "resultToken",
                "expires_in": 10,
                "token_type": "bearer"
              }"""
            ),
            Right(
              json"""{
                "access_token": "otherToken",
                "expires_in": 10,
                "token_type": "bearer"
              }"""
            )
          ))
        val service = AccessTokenAuth.instance[IO](ExampleCredentialsConfig)
        val result1 = service.token.unsafeRunSync()
        val result2 = service.token.unsafeRunSync()
        result1 shouldBe "resultToken"
        result2 shouldBe "otherToken"
        backend.getCalls.unsafeRunSync().length shouldBe 2
      }

      "asked for refresh" in {
        implicit val cacheMode: Mode[IO] = scalacache.CatsEffect.modes.async
        implicit val backend: HttpBackendFake[IO] = httpBackend(
          Seq(
            Right(
              json"""{
                "access_token": "resultToken",
                "expires_in": 10,
                "token_type": "bearer"
              }"""
            ),
            Right(
              json"""{
                "access_token": "otherToken",
                "expires_in": 10,
                "token_type": "bearer"
              }"""
            )
          ))
        val service = AccessTokenAuth.withCache[IO](ExampleCredentialsConfig)
        val result1 = service.refresh.unsafeRunSync()
        val result2 = service.refresh.unsafeRunSync()
        result1 shouldBe AccessTokenResponse("resultToken", 10, "bearer")
        result2 shouldBe AccessTokenResponse("otherToken", 10, "bearer")
        backend.getCalls.unsafeRunSync().length shouldBe 2
      }
    }
  }
}
