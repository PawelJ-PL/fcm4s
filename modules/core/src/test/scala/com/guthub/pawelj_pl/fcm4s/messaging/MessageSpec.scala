package com.guthub.pawelj_pl.fcm4s.messaging

import com.github.pawelj_pl.fcm4s.messaging.{DataMessage, Destination, Message, NotificationMessage}
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
    }

    "compile" when {
      "DataMessage is provided" in {
        val data = Map("foo" -> "1", "bar" -> "2")
        val message = DataMessage(Destination.Token("someToken"), data)

        def fn(msg: Message) = msg.asJson

        fn(message)
      }

      "NotificationMessage is provided" in {
        val message = NotificationMessage(Destination.Token("someToken"))

        def fn(msg: Message) = msg.asJson

        fn(message)
      }
    }
  }
}
