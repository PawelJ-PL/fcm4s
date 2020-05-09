package com.guthub.pawelj_pl.fcm4s.messaging

import java.time.Instant

import com.github.pawelj_pl.fcm4s.messaging.extrafields.{
  Android,
  AndroidNotification,
  AndroidNotificationPriority,
  AndroidNotificationVisibility,
  Apns,
  ApnsFcmOptions,
  ApnsPayloadAlert,
  ApnsPayloadSound,
  Color,
  LightSettings,
  MessagePriority,
  StandardApnsPayload,
  Webpush,
  WebpushFcmOptions,
  WebpushNotification,
  WebpushNotificationDirection
}
import com.github.pawelj_pl.fcm4s.messaging.{DataMessage, Destination, FcmOptions, Message, MessageDataEncoder, NotificationMessage}
import io.circe.generic.semiauto.deriveEncoder
import io.circe.{Encoder, Json}
import io.circe.literal._
import io.circe.syntax._
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration.Duration

class MessageSpec extends WordSpec with Matchers {
  private val ExampleAndroidNotification = AndroidNotification(
    title = Some("Android title"),
    body = Some("Android body"),
    icon = Some("Android icon"),
    color = Some("Android color"),
    sound = Some("Android sound"),
    tag = Some("Android tag"),
    clickAction = Some("Android click action"),
    bodyLocKey = Some("Android body loc key"),
    titleLocKey = Some("Android title loc key"),
    titleLocArgs = Some(List("foo", "bar")),
    channelId = Some("Android channel Id"),
    ticker = Some("Android ticker"),
    sticky = Some(true),
    eventTime = Some(Instant.EPOCH.plusNanos(123456)),
    localOnly = Some(true),
    notificationPriority = Some(AndroidNotificationPriority.PriorityMax),
    defaultSound = Some(true),
    defaultVibrateTimings = Some(true),
    defaultLightSettings = Some(true),
    vibrateTimings = Some(List(Duration(3, "seconds"), Duration(120123, "nano"))),
    visibility = Some(AndroidNotificationVisibility.VisibilityUnspecified),
    notificationCount = Some(7),
    lightSettings = Some(LightSettings(Color(0.3, 0.2, 0.1, 1.0), Duration(300, "millisecond"), Duration(10, "second"))),
    image = Some("Android image")
  )

  private val ExampleAndroidConfig = Android(
    collapseKey = Some("someKey"),
    priority = Some(MessagePriority.High),
    ttl = Some(Duration(2200, "millisecond")),
    restrictedPackageName = Some("some.package"),
    data = Some(Map("bazQux" -> "quux", "aB" -> "C")),
    notification = Some(ExampleAndroidNotification),
    fcmOptions = Some(FcmOptions(analyticsLabel = Some("someLabel")))
  )

  private val ExampleWebpushNotification = WebpushNotification(
    actions = Some(Map("foo" -> "bar", "baz" -> "qux")),
    badge = Some("someBadge"),
    body = Some("someBody"),
    data = Some(Map("a" -> "b", "c" -> "d")),
    dir = Some(WebpushNotificationDirection.Ltr),
    lang = Some("pl-PL"),
    tag = Some("someTag"),
    icon = Some("someIcon"),
    image = Some("someImage"),
    renotify = Some(true),
    requireInteraction = Some(true),
    silent = Some(true),
    timestamp = Some(1589227678407L),
    title = Some("someTitle"),
    vibrate = Some(List(300, 200, 300))
  )

  private val ExampleWebpushConfig = Webpush(
    headers = Some(Map("h1" -> "v1", "h2" -> "v2")),
    data = Some(Map("key1" -> "value1", "key2" -> "value2")),
    notification = Some(ExampleWebpushNotification),
    fcmOptions = Some(WebpushFcmOptions(link = Some("someLink"), analyticsLabel = Some("someLabel")))
  )

  private val ExampleApnsPayladAlert = ApnsPayloadAlert.AlertDict(
    title = Some("someTitle"),
    subtitle = Some("someSubtitle"),
    body = Some("someBody"),
    launchImage = Some("someLaunchImage"),
    titleLocKey = Some("someTitleLocKey"),
    titleLocArgs = Some(List("titleLoc1", "titleLoc2")),
    subtitleLocKey = Some("someSubtitleLocKey"),
    subtitleLocArgs = Some(List("subtitleLoc1", "subtitleLoc2")),
    locKey = Some("someLocKey"),
    locArgs = Some(List("loc1", "loc2"))
  )

  private val ExampleApnsPayload = StandardApnsPayload(
    alert = Some(ExampleApnsPayladAlert),
    badge = Some(3),
    sound = Some(ApnsPayloadSound.SoundDict(critical = 1, name = "someSound", volume = 1.0)),
    threadId = Some("someThread"),
    category = Some("someCategory"),
    contentAvailable = Some(1),
    mutableContent = Some(1),
    targetContentId = Some("someTargetContentId")
  )

  private val ExampleApnsConfig = Apns.withStandardPayload(
    headers = Some(Map("h1" -> "v1", "h2" -> "v2")),
    payload = ExampleApnsPayload,
    fcmOptions = Some(ApnsFcmOptions(analyticsLabel = Some("someLabel"), image = Some("someImage")))
  )

  "Message encoder" should {
    "encode DataMessage" which {
      "has token destination" in {
        val data = Map("foo" -> "1", "bar" -> "2")
        val message = DataMessage(Destination.Token("someToken"), data, name = Some("someName"))

        val result = message.asJson

        result shouldBe
          json"""{
                "message": {
                  "token": "someToken",
                  "data": {
                    "foo": "1",
                    "bar": "2"
                  },
                  "name": "someName"
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

      "has name" in {
        val data = Map("foo" -> "1", "bar" -> "2")
        val message = DataMessage(Destination.Token("someToken"), data, name = Some("My name"))

        val result = message.asJson

        result shouldBe
          json"""{
                "message": {
                  "token": "someToken",
                  "data": {
                    "foo": "1",
                    "bar": "2"
                  },
                  "name": "My name"
                }
              }"""
      }

      "has Android config" in {
        val data = Map("foo" -> "1", "fooBar" -> "2")
        val message = DataMessage(Destination.Token("someToken"), data, android = Some(ExampleAndroidConfig.copy(notification = None)))

        val result = message.asJson

        result shouldBe
          json"""{
                "message": {
                  "token": "someToken",
                  "data": {
                    "foo": "1",
                    "fooBar": "2"
                  },
                  "android": {
                    "collapse_key": "someKey",
                    "priority": "high",
                    "ttl": "2.2s",
                    "restricted_package_name": "some.package",
                    "data": {
                      "bazQux": "quux",
                      "aB": "C"
                    },
                    "fcm_options": {
                      "analytics_label": "someLabel"
                    }
                  }
                }
              }"""
      }

      "has webpush config" in {
        val data = Map("foo" -> "1", "bar" -> "2")
        val message = DataMessage(Destination.Token("someToken"), data, webpush = Some(ExampleWebpushConfig.copy(notification = None)))

        val result = message.asJson

        result shouldBe
          json"""{
                "message": {
                  "token": "someToken",
                  "data": {
                    "foo": "1",
                    "bar": "2"
                  },
                  "webpush": {
                    "headers": {
                      "h1": "v1",
                      "h2": "v2"
                    },
                    "data": {
                      "key1": "value1",
                      "key2": "value2"
                    },
                    "fcm_options": {
                      "link": "someLink",
                      "analytics_label": "someLabel"
                    }
                  }
                }
              }"""
      }

      "has apns config" in {
        val data = Map("foo" -> "1", "bar" -> "2")
        val message = DataMessage(Destination.Token("someToken"), data, apns = Some(ExampleApnsConfig))

        val result = message.asJson

        result shouldBe
          json"""{
                "message": {
                  "token": "someToken",
                  "data": {
                    "foo": "1",
                    "bar": "2"
                  },
                  "apns": {
                    "headers": {
                      "h1": "v1",
                      "h2": "v2"
                    },
                    "payload": {
                      "aps": {
                        "alert": {
                          "title": "someTitle",
                          "subtitle": "someSubtitle",
                          "body": "someBody",
                          "launch-image": "someLaunchImage",
                          "title-loc-key": "someTitleLocKey",
                          "title-loc-args": [
                            "titleLoc1",
                            "titleLoc2"
                          ],
                          "subtitle-loc-key": "someSubtitleLocKey",
                          "subtitle-loc-args": [
                            "subtitleLoc1",
                            "subtitleLoc2"
                          ],
                          "loc-key": "someLocKey",
                          "loc-args": [
                            "loc1",
                            "loc2"
                          ]
                        },
                        "badge": 3,
                        "sound": {
                          "critical": 1,
                          "name": "someSound",
                          "volume": 1.0
                        },
                        "thread-id": "someThread",
                        "category": "someCategory",
                        "content-available": 1,
                        "mutable-content": 1,
                        "target-content-id": "someTargetContentId"
                      }
                    },
                    "fcm_options": {
                      "analytics_label": "someLabel",
                      "image": "someImage"
                    }
                  }
                }
              }"""
      }

      "has FCM Options config" in {
        val data = Map("foo" -> "1", "bar" -> "2")
        val message = DataMessage(Destination.Token("someToken"), data, fcmOptions = Some(FcmOptions(Some("someLabel"))))

        val result = message.asJson

        result shouldBe
          json"""{
                "message": {
                  "token": "someToken",
                  "data": {
                    "foo": "1",
                    "bar": "2"
                  },
                  "fcm_options": {
                    "analytics_label": "someLabel"
                  }
                }
              }"""
      }
    }

    "Encode NotificationMessage" which {
      "has all fields defined" in {
        val data = Map("foo" -> "1", "bar" -> "2")
        val message =
          NotificationMessage(Destination.Topic("someTopic"),
                              Some("someTitle"),
                              Some("someBody"),
                              Some("someImage"),
                              Some(data),
                              name = Some("someName"))

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
                  },
                  "name": "someName"
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
                    "body": "someBody"
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
                    "foo": "122",
                    "bar": "something",
                    "baz": "true"
                  }
                }
              } """
      }

      "has android config defined" in {
        val data = Map("foo" -> "1", "bar" -> "2")
        val message =
          NotificationMessage(Destination.Topic("someTopic"),
                              Some("someTitle"),
                              Some("someBody"),
                              Some("someImage"),
                              Some(data),
                              android = Some(ExampleAndroidConfig))

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
                  },
                  "android": {
                    "collapse_key": "someKey",
                    "priority": "high",
                    "ttl": "2.2s",
                    "restricted_package_name": "some.package",
                    "data": {
                      "bazQux": "quux",
                      "aB": "C"
                    },
                    "notification": {
                      "title": "Android title",
                      "body": "Android body",
                      "icon": "Android icon",
                      "color": "Android color",
                      "sound": "Android sound",
                      "tag": "Android tag",
                      "click_action": "Android click action",
                      "body_loc_key": "Android body loc key",
                      "title_loc_key": "Android title loc key",
                      "title_loc_args": [
                        "foo",
                        "bar"
                      ],
                      "channel_id": "Android channel Id",
                      "ticker": "Android ticker",
                      "sticky": true,
                      "event_time": "1970-01-01T00:00:00.000123456Z",
                      "local_only": true,
                      "notification_priority": "PRIORITY_MAX",
                      "default_sound": true,
                      "default_vibrate_timings": true,
                      "default_light_settings": true,
                      "vibrate_timings": [
                        "3s",
                        "0.000120123s"
                      ],
                      "visibility": "VISIBILITY_UNSPECIFIED",
                      "notification_count": 7,
                      "light_settings": {
                        "color": {
                          "red": 0.3,
                          "green": 0.2,
                          "blue": 0.1,
                          "alpha": 1.0
                        },
                        "light_on_duration": "0.3s",
                        "light_off_duration": "10s"
                      },
                      "image": "Android image"
                    },
                    "fcm_options": {
                      "analytics_label": "someLabel"
                    }
                  }
                }
              } """
      }

      "has webpush config defined" in {
        val data = Map("foo" -> "1", "bar" -> "2")
        val message =
          NotificationMessage(Destination.Topic("someTopic"),
                              Some("someTitle"),
                              Some("someBody"),
                              Some("someImage"),
                              Some(data),
                              webpush = Some(ExampleWebpushConfig))

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
                  },
                  "webpush": {
                    "headers": {
                      "h1": "v1",
                      "h2": "v2"
                    },
                    "data": {
                      "key1": "value1",
                      "key2": "value2"
                    },
                    "notification": {
                      "actions": {
                        "foo": "bar",
                        "baz": "qux"
                      },
                      "badge": "someBadge",
                      "body": "someBody",
                      "data": {
                        "a": "b",
                        "c": "d"
                      },
                      "dir": "ltr",
                      "lang": "pl-PL",
                      "tag": "someTag",
                      "icon": "someIcon",
                      "image": "someImage",
                      "renotify": true,
                      "requireInteraction": true,
                      "silent": true,
                      "timestamp": 1589227678407,
                      "title": "someTitle",
                      "vibrate": [
                        300,
                        200,
                        300
                      ]
                    },
                    "fcm_options": {
                      "link": "someLink",
                      "analytics_label": "someLabel"
                    }
                  }
                }
              } """
      }

      "has apns config defined" in {
        val data = Map("foo" -> "1", "bar" -> "2")
        val message =
          NotificationMessage(Destination.Topic("someTopic"),
                              Some("someTitle"),
                              Some("someBody"),
                              Some("someImage"),
                              Some(data),
                              apns = Some(ExampleApnsConfig))

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
                  },
                  "apns": {
                    "headers": {
                      "h1": "v1",
                      "h2": "v2"
                    },
                    "payload": {
                      "aps": {
                        "alert": {
                          "title": "someTitle",
                          "subtitle": "someSubtitle",
                          "body": "someBody",
                          "launch-image": "someLaunchImage",
                          "title-loc-key": "someTitleLocKey",
                          "title-loc-args": [
                            "titleLoc1",
                            "titleLoc2"
                          ],
                          "subtitle-loc-key": "someSubtitleLocKey",
                          "subtitle-loc-args": [
                            "subtitleLoc1",
                            "subtitleLoc2"
                          ],
                          "loc-key": "someLocKey",
                          "loc-args": [
                            "loc1",
                            "loc2"
                          ]
                        },
                        "badge": 3,
                        "sound": {
                          "critical": 1,
                          "name": "someSound",
                          "volume": 1.0
                        },
                        "thread-id": "someThread",
                        "category": "someCategory",
                        "content-available": 1,
                        "mutable-content": 1,
                        "target-content-id": "someTargetContentId"
                      }
                    },
                    "fcm_options": {
                      "analytics_label": "someLabel",
                      "image": "someImage"
                    }
                  }
                }
              } """
      }

      "has FCM options config defined" in {
        val data = Map("foo" -> "1", "bar" -> "2")
        val message =
          NotificationMessage(
            Destination.Topic("someTopic"),
            Some("someTitle"),
            Some("someBody"),
            Some("someImage"),
            Some(data),
            name = Some("someName"),
            fcmOptions = Some(FcmOptions(Some("someLabel")))
          )

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
                  },
                  "name": "someName",
                  "fcm_options": {
                    "analytics_label": "someLabel"
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

    "generate apns config" when {
      val headers = Map("foo" -> "bar", "baz" -> "qux")
      val fcmOptions = ApnsFcmOptions(analyticsLabel = Some("someLabel"), image = Some("someImage"))

      case class TestPayload(x: String, y: Int, z: Boolean)
      implicit val encoder: Encoder[TestPayload] = deriveEncoder[TestPayload]

      val exampleStandardPayload = StandardApnsPayload(
        alert = Some(ApnsPayloadAlert.AlertDict(title = Some("someTitle"), launchImage = Some("someImage"))),
        sound = Some(ApnsPayloadSound.Default),
        threadId = Some("someThread")
      )

      val exampleCustomPayload = TestPayload("a", 6, z = false)

      "JSON payload provided directly" in {
        val payload = Json.obj(
          ("x", Json.fromInt(1)),
          ("y", Json.fromString("z"))
        )
        val apns = Apns(Some(headers), Some(payload), Some(fcmOptions))
        apns.asJson shouldBe
          json"""{
                "headers": {
                  "foo": "bar",
                  "baz": "qux"
                },
                "payload": {
                  "x": 1,
                  "y": "z"
                },
                "fcm_options": {
                  "analytics_label" : "someLabel",
                  "image" : "someImage"
                }
              }"""
      }

      "JSONable payload provided" in {
        val apns = Apns.withCustomPayload(Some(headers), exampleCustomPayload, Some(fcmOptions))
        apns.asJson shouldBe
          json"""{
                "headers": {
                  "foo": "bar",
                  "baz": "qux"
                },
                "payload": {
                  "x": "a",
                  "y": 6,
                  "z": false
                },
                "fcm_options": {
                  "analytics_label" : "someLabel",
                  "image" : "someImage"
                }
              }"""
      }

      "standard payload provided" in {
        val apns = Apns.withStandardPayload(headers = Some(headers), exampleStandardPayload, Some(fcmOptions))
        apns.asJson shouldBe
          json"""{
                "headers": {
                  "foo": "bar",
                  "baz": "qux"
                },
                "payload": {
                  "aps": {
                    "alert": {
                      "title": "someTitle",
                      "launch-image": "someImage"
                    },
                    "sound": "default",
                    "thread-id": "someThread"
                  }
                },
                "fcm_options": {
                  "analytics_label" : "someLabel",
                  "image" : "someImage"
                }
              }"""
      }

      "mixed payload provided" in {
        val apns = Apns.withMixedPayload(
          headers = Some(headers),
          exampleStandardPayload,
          exampleCustomPayload,
          Some(fcmOptions)
        )
        apns.asJson shouldBe
          json"""{
                "headers": {
                  "foo": "bar",
                  "baz": "qux"
                },
                "payload": {
                  "aps": {
                    "alert": {
                      "title": "someTitle",
                      "launch-image": "someImage"
                    },
                    "sound": "default",
                    "thread-id": "someThread"
                  },
                  "x": "a",
                  "y": 6,
                  "z": false
                },
                "fcm_options": {
                  "analytics_label" : "someLabel",
                  "image" : "someImage"
                }
              }"""
      }
    }
  }
}
