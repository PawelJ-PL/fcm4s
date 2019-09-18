package com.guthub.pawelj_pl.fcm4s.messaging

import cats.effect.IO
import com.github.pawelj_pl.fcm4s.Common.ErrorResponse
import com.github.pawelj_pl.fcm4s.auth.{AccessTokenAuth, AccessTokenResponse}
import com.github.pawelj_pl.fcm4s.auth.config.CredentialsConfig
import com.github.pawelj_pl.fcm4s.messaging.{Destination, Messaging, NotificationMessage}
import com.guthub.pawelj_pl.fcm4s.testhelpers.fakes.HttpBackendFake
import io.circe.Json
import io.circe.literal._
import org.http4s.Uri
import org.scalatest.{Matchers, WordSpec}

class MessagingSpec extends WordSpec with Matchers {
  final val ExampleRsaKey =
    "-----BEGIN PRIVATE KEY-----\nMIIBPAIBAAJBAL2JdwKahOm9IG7UBkPU99oEWV4S70K3BjbJ0AQLKpctYyho33lb\naKNhGWzlazrEiyKqSOU/xSwQrdd6AnFSe3kCAwEAAQJBALntfHpoW9PyvDsb8F1g\nMBaFR6l6B405f3YFePJOheQvZzqPopnQpYymwVK72HDccV30GUt0TbSWWilbMgJa\nawECIQDvVcp/k7Gw+l1QSwbi7cShcpyMeVVP6+s3OJ1PKWTQWQIhAMq8AmDc6t6o\nrt2AmtRWri9bBAr06jHOu/b6ZTXefKAhAiEAgMJM8RnKTQZE0X+rssZsNNduNXzJ\nUvf/UXQZ3Y7Nd/ECIQCjkLpuge5wxDGI/jhstp6EEG+bk2vb0YqvQegkZSOxYQIg\ncdSrzvFvbDPn4g4QNK/MQAcCPU65WbMVxHNrqc0fhlI=\n-----BEGIN PRIVATE KEY-----"
  final val ExampleCredentialsConfig =
    CredentialsConfig("client1", "some@example.org", Uri.uri("http://localhost/token"), ExampleRsaKey, "key1", "project1")
  final val ExampleMessage = NotificationMessage(Destination.Topic("someTopic"))

  def httpBackend(responses: Seq[Either[(Int, String), Json]]): HttpBackendFake[IO] =
    HttpBackendFake.instanceF[IO](HttpBackendFake.BackendState(responses = responses)).unsafeRunSync()

  implicit val authService: AccessTokenAuth[IO] = new AccessTokenAuth[IO] {
    override def token: IO[String] = IO("someToken")

    override def refresh: IO[AccessTokenResponse] = IO(AccessTokenResponse("someToken", 10, "bearer"))
  }

  "Messaging" should {
    "send message and return message name" when {
      "got success response" in {
        implicit val backend: HttpBackendFake[IO] = httpBackend(
          Seq(
            Right(
              json"""{
                "name": "someName"
              }"""
            )
          ))

        val service = Messaging.create[IO](ExampleCredentialsConfig)
        val result = service.send(ExampleMessage).unsafeRunSync()

        result shouldBe Right("someName")
      }
    }

    "send message and return code and body" when {
      "got unexpected status code" in {
        implicit val backend: HttpBackendFake[IO] = httpBackend(
          Seq(
            Right(
              json"""{
                "foo": "bar"
              }"""
            )
          ))

        val service = Messaging.create[IO](ExampleCredentialsConfig)
        val result = service.send(ExampleMessage).unsafeRunSync()

        result shouldBe Left(ErrorResponse(400, """unable to decode body: {
                                                  |  "foo" : "bar"
                                                  |}, error: Attempt to decode value on failed cursor""".stripMargin))
      }
    }

    "Send many messages and return responses" in {
      implicit val backend: HttpBackendFake[IO] = httpBackend(
        Seq(
          Right(
            json"""{
                "name": "someName"
              }"""
          ),
          Left((503, "Unknown error")),
          Right(
            json"""{
                "name": "otherName"
              }"""
          )
        ))

      val service = Messaging.create[IO](ExampleCredentialsConfig)
      val result = service.sendMany(ExampleMessage, ExampleMessage, ExampleMessage).unsafeRunSync()

      result should contain theSameElementsAs List(
        Right("someName"),Left(ErrorResponse(503, "Unknown error")), Right("otherName")
      )
    }
  }
}