package com.guthub.pawelj_pl.fcm4s.messaging

import cats.effect.IO
import com.github.pawelj_pl.fcm4s.Common.ErrorResponse
import com.github.pawelj_pl.fcm4s.auth.config.CredentialsConfig
import com.github.pawelj_pl.fcm4s.messaging.{Destination, MessageDataEncoder, Messaging, NotificationMessage}
import com.guthub.pawelj_pl.fcm4s.testhelpers.fakes.{AccessTokenAuthFake, AuthCall, HttpBackendFake}
import io.circe.Json
import io.circe.literal._
import org.http4s.Uri
import org.scalatest.{Matchers, WordSpec}

class MessagingSpec extends WordSpec with Matchers {
  final val ExampleRsaKey =
    "-----BEGIN PRIVATE KEY-----\nMIIBPAIBAAJBAL2JdwKahOm9IG7UBkPU99oEWV4S70K3BjbJ0AQLKpctYyho33lb\naKNhGWzlazrEiyKqSOU/xSwQrdd6AnFSe3kCAwEAAQJBALntfHpoW9PyvDsb8F1g\nMBaFR6l6B405f3YFePJOheQvZzqPopnQpYymwVK72HDccV30GUt0TbSWWilbMgJa\nawECIQDvVcp/k7Gw+l1QSwbi7cShcpyMeVVP6+s3OJ1PKWTQWQIhAMq8AmDc6t6o\nrt2AmtRWri9bBAr06jHOu/b6ZTXefKAhAiEAgMJM8RnKTQZE0X+rssZsNNduNXzJ\nUvf/UXQZ3Y7Nd/ECIQCjkLpuge5wxDGI/jhstp6EEG+bk2vb0YqvQegkZSOxYQIg\ncdSrzvFvbDPn4g4QNK/MQAcCPU65WbMVxHNrqc0fhlI=\n-----BEGIN PRIVATE KEY-----"
  final val ExampleCredentialsConfig =
    CredentialsConfig("client1", "some@example.org", Uri.uri("http://localhost/token"), ExampleRsaKey, "key1", "project1")
  final val ExampleMessage = NotificationMessage(Destination.Topic("someTopic"), data = Some(Map("foo" -> "bar")))

  def httpBackend(responses: Seq[Either[(Int, String), Json]]): HttpBackendFake[IO] =
    HttpBackendFake.instanceF[IO](HttpBackendFake.BackendState(responses = responses)).unsafeRunSync()

  def accessTokenAuth: AccessTokenAuthFake[IO] = AccessTokenAuthFake.instanceF[IO].unsafeRunSync()

  "Messaging" should {
    implicit val auth: AccessTokenAuthFake[IO] = accessTokenAuth
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
        implicit val auth: AccessTokenAuthFake[IO] = accessTokenAuth
        implicit val backend: HttpBackendFake[IO] = httpBackend(
          Seq(
            Left(403, "Forbidden")
          ))

        val service = Messaging.create[IO](ExampleCredentialsConfig)
        val result = service.send(ExampleMessage).unsafeRunSync()

        result shouldBe Left(ErrorResponse(403, "Forbidden"))
      }
    }

    "Send many messages with single auth request and return responses" in {
      implicit val auth: AccessTokenAuthFake[IO] = accessTokenAuth
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
        Right("someName"),
        Left(ErrorResponse(503, "Unknown error")),
        Right("otherName")
      )
      auth.getCalls.unsafeRunSync() should contain theSameElementsAs List(AuthCall.Token)
    }

    "Send many messages with custom message data type" in {
      case class Data(foo: String, bar: Int)
      implicit val dataEncoder: MessageDataEncoder[Data] =
        MessageDataEncoder.deriveFrom[Data](data => Map("foo" -> data.foo, "bar" -> data.bar.toString))

      val m1 = ExampleMessage.copy(data = Some(Data("abc", 123)))
      val m2 = ExampleMessage.copy(data = Some(Data("xyz", 999)))

      implicit val auth: AccessTokenAuthFake[IO] = accessTokenAuth
      implicit val backend: HttpBackendFake[IO] = httpBackend(
        Seq(
          Right(
            json"""{
                "name": "someName"
              }"""
          ),
          Right(
            json"""{
                "name": "otherName"
              }"""
          )
        ))

      val service = Messaging.create[IO](ExampleCredentialsConfig)
      service.sendMany(ExampleMessage)
      val result = service.sendMany(m1, m2).unsafeRunSync()

      result should contain theSameElementsAs List(
        Right("someName"),
        Right("otherName")
      )
      auth.getCalls.unsafeRunSync() should contain theSameElementsAs List(AuthCall.Token)
    }
  }
}
