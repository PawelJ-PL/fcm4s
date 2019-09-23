package com.guthub.pawelj_pl.fcm4s.messaging

import com.github.pawelj_pl.fcm4s.messaging.{DataMessage, Destination, Message, MessageDataEncoder, NotificationMessage}
import io.circe.literal._
import io.circe.syntax._
import org.scalatest.{Matchers, WordSpec}

class MessageSpec extends WordSpec with Matchers {
  "Message encoder" should {
    "encode DataMessage" which {
      "has token destination" in {
        val data = Map("foo" -> "1", "bar" -> "2")
        val message = DataMessage(Destination.Token("someToken"), data)

        val result = message.asJson

        result shouldBe
          json"""{
                "message": {
                  "token": "someToken",
                  "data": {
                    "foo": "1",
                    "bar": "2"
                  }
                }
              }"""
      }

      "has topic destination" in {
        val data = Map("foo" -> "1", "bar" -> "2")
        val message = DataMessage(Destination.Topic("someTopic"), data)

        val result = message.asJson

        result shouldBe
          json"""{
                "message": {
                  "topic": "someTopic",
                  "data": {
                    "foo": "1",
                    "bar": "2"
                  }
                }
              }"""
      }

      "has condition destination" in {
        val data = Map("foo" -> "1", "bar" -> "2")
        val message = DataMessage(Destination.Condition("'TopicB' in topics || 'TopicC' in topics"), data)

        val result = message.asJson

        result shouldBe
          json"""{
                "message": {
                  "condition": "'TopicB' in topics || 'TopicC' in topics",
                  "data": {
                    "foo": "1",
                    "bar": "2"
                  }
                }
              }"""
      }

      "has custom data type" in {
        case class Data(foo: Int, bar: String, baz: Option[Boolean])
        val data = Data(122, "something", Some(true))
        implicit val dataEncoder: MessageDataEncoder[Data] = MessageDataEncoder.deriveFrom[Data](data =>
          Map("foo" -> data.foo.toString, "bar" -> data.bar, "baz" -> data.baz.map(_.toString).getOrElse("false")))
        val message = DataMessage(Destination.Token("someToken"), data)

        val result = message.asJson

        result shouldBe
          json"""{
                "message": {
                  "token": "someToken",
                  "data": {
                    "foo": "122",
                    "bar": "something",
                    "baz": "true"
                  }
                }
              }"""
      }
    }

    "Encode NotificationMessage" which {
      "has all fields defined" in {
        val data = Map("foo" -> "1", "bar" -> "2")
        val message =
          NotificationMessage(Destination.Topic("someTopic"), Some("someTitle"), Some("someBody"), Some("someImage"), Some(data))

        val result = message.asJson

        result shouldBe
          json"""{
                "message": {
                  "topic": "someTopic",
                  "notification": {
                    "title": "someTitle",
                    "body": "someBody",
                    "image": "someImage"
                  },
                  "data": {
                    "foo": "1",
                    "bar": "2"
                  }
                }
              } """
      }

      "has no title defined" in {
        val data = Map("foo" -> "1", "bar" -> "2")
        val message = NotificationMessage(Destination.Topic("someTopic"), None, Some("someBody"), Some("someImage"), Some(data))

        val result = message.asJson

        result shouldBe
          json"""{
                "message": {
                  "topic": "someTopic",
                  "notification": {
                    "title": null,
                    "body": "someBody",
                    "image": "someImage"
                  },
                  "data": {
                    "foo": "1",
                    "bar": "2"
                  }
                }
              } """
      }
      "has no body defined" in {
        val data = Map("foo" -> "1", "bar" -> "2")
        val message = NotificationMessage(Destination.Topic("someTopic"), Some("someTitle"), None, Some("someImage"), Some(data))

        val result = message.asJson

        result shouldBe
          json"""{
                "message": {
                  "topic": "someTopic",
                  "notification": {
                    "title": "someTitle",
                    "body": null,
                    "image": "someImage"
                  },
                  "data": {
                    "foo": "1",
                    "bar": "2"
                  }
                }
              } """
      }
      "has no image defined" in {
        val data = Map("foo" -> "1", "bar" -> "2")
        val message = NotificationMessage(Destination.Topic("someTopic"), Some("someTitle"), Some("someBody"), None, Some(data))

        val result = message.asJson

        result shouldBe
          json"""{
                "message": {
                  "topic": "someTopic",
                  "notification": {
                    "title": "someTitle",
                    "body": "someBody",
                    "image": null
                  },
                  "data": {
                    "foo": "1",
                    "bar": "2"
                  }
                }
              } """
      }
      "has no data defined" in {
        val message = NotificationMessage(Destination.Topic("someTopic"), Some("someTitle"), Some("someBody"), Some("someImage"), None)

        val result = message.asJson

        result shouldBe
          json"""{
                "message": {
                  "topic": "someTopic",
                  "notification": {
                    "title": "someTitle",
                    "body": "someBody",
                    "image": "someImage"
                  },
                  "data": null
                }
              } """
      }

      "has custom data type" in {
        case class Data(foo: Int, bar: String, baz: Option[Boolean])
        val data = Data(122, "something", Some(true))
        implicit val dataEncoder: MessageDataEncoder[Data] = MessageDataEncoder.deriveFrom[Data](data =>
          Map("foo" -> data.foo.toString, "bar" -> data.bar, "baz" -> data.baz.map(_.toString).getOrElse("false")))
        val message = NotificationMessage(Destination.Topic("someTopic"), Some("someTitle"), Some("someBody"), Some("someImage"), Some(data))

        val result = message.asJson

        result shouldBe
          json"""{
                "message": {
                  "topic": "someTopic",
                  "notification": {
                    "title": "someTitle",
                    "body": "someBody",
                    "image": "someImage"
                  },
                  "data": {
                    "foo": "122",
                    "bar": "something",
                    "baz": "true"
                  }
                }
              } """
      }
    }

    "compile" when {
      "DataMessage is provided" in {
        val data = Map("foo" -> "1", "bar" -> "2")
        val message = DataMessage(Destination.Token("someToken"), data)

        def fn(msg: Message[Map[String, String]]) = msg.asJson

        fn(message)
      }

      "NotificationMessage is provided" in {
        val message = NotificationMessage(Destination.Token("someToken"))

        def fn(msg: Message[Map[String, String]]) = msg.asJson

        fn(message)
      }
    }
  }
}
