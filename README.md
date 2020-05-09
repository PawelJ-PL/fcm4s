# Fcm4s

[![Maven Central](https://img.shields.io/maven-central/v/com.github.pawelj-pl/fcm4s-core_2.13.svg)](https://img.shields.io/maven-central/v/com.github.pawelj-pl/fcm4s-core_2.13.svg)

FCM ([Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging)) library for Scala. At the moment it supports only sending messages without platform customization (support for platform config will be added in the future).

## Getting started

### Dependencies

You can start using library by adding following dependencies to `build.sbt`:

```sbt
libraryDependencies += "com.github.pawelj-pl" %% "fcm4s-core" % "<libraryVersion>"
libraryDependencies += "com.github.pawelj-pl" %% "fcm4s-http4s" % "<libraryVersion>"
```

### Basic usage

```scala
package com.github.pawelj_pl.fcm4s.http4s

import cats.effect.{ContextShift, ExitCode, IO, IOApp}
import com.github.pawelj_pl.fcm4s.messaging.{DataMessage, Destination, Messaging, NotificationMessage}

import scala.concurrent.ExecutionContext.global

object Main extends IOApp {
  private implicit val cs: ContextShift[IO] = IO.contextShift(global)
  val result = Http4sBackend.create[IO](global).use(implicit backend => {
    val m1 = DataMessage(Destination.Topic("abc"), Map("foo" -> "a", "bar" -> "b"))
    val m2 = NotificationMessage(Destination.Token("abc"), title = Some("Some title"), body = Some("Body"), image = None, data = Some(Map("foo" -> "a", "bar" -> "b")))
    val m3 = NotificationMessage(Destination.Condition("'TopicB' in topics || 'TopicC' in topics"))
    for {
      messaging <- Messaging.defaultIoMessaging("/tmp/config.json")
      send1     <- messaging.send(m1)
      send2     <- messaging.sendMany(m1, m2, m3)
      send3     <- messaging.sendMany(List(m1, m2, m3))
    } yield (send1, send2, send3)
  })

  override def run(args: List[String]): IO[ExitCode] = result.map(sent => {
    println(sent)
    ExitCode.Success
  })
}
```

## Credentials config

Instance of `com.github.pawelj_pl.fcm4s.auth.config.CredentialsConfig` is required by authentication and message service. In the simplest scenario it will be loaded from service account credentials file (see [documentation](https://firebase.google.com/docs/admin/setup), section **To generate a private key file for your service account**) here:
```scala
Messaging.defaultIoMessaging("/tmp/config.json")
```

You can also load config manually:

```scala
val credentialsConfig: IO[Either[ConfigError, CredentialsConfig]] = CredentialsConfig.fromFile[IO]("/tmp/config.json")
```

or use [Pureconfig](https://github.com/pureconfig/pureconfig) to create config from environment variables (for example for Heroku deployment)

and next pass it directly:

```scala
val credentialsConfig: CredentialsConfig = ???
Messaging.defaultIoMessaging(credentialsConfig)
```

## Message types

There are two types of message: `Notification message` and `Data message`. For more details see [Firebase documentation](https://firebase.google.com/docs/cloud-messaging/concept-options).
Examples:

```scala
import com.github.pawelj_pl.fcm4s.messaging.{DataMessage, Destination, NotificationMessage}

val m1 = DataMessage(Destination.Topic("abc"), Map("foo" -> "a", "bar" -> "b")) // Data message with topic destination and key value data
val m2 = NotificationMessage(Destination.Token("abc"), title = Some("Some title"), body = Some("Body"), image = Some("https://upload.wikimedia.org/wikipedia/commons/6/63/Wikipedia-logo.png"), data = Some(Map("foo" -> "a", "bar" -> "b"))) //Mixed message with token destination, notification title, body, image and key value data
val m3 = NotificationMessage(Destination.Condition("'TopicB' in topics || 'TopicC' in topics"), title = Some("example title"))  //Notification message with condition destination and title only
```

##### Message data
Both Notification message and data message can contain payload passed to message (for MessageData it's required). It can be any type with instance of `MessageDataEncoder`. There is default instance for type `Map[String, String]`. For any other type custom instance have to be created. For example:

```scala
import com.github.pawelj_pl.fcm4s.messaging.{Destination, MessageDataEncoder, NotificationMessage}

case class Data(foo: String, bar: Int)
implicit val dataEncoder: MessageDataEncoder[Data] = MessageDataEncoder.deriveFrom[Data](data => Map("foo" -> data.foo, "bar" -> data.bar.toString))
val ExampleMessage = NotificationMessage(Destination.Topic("someTopic"))
```

## Http backend

The library require implementation of `com.github.pawelj_pl.fcm4s.http.HttpBackend`. There are no built-in implementation, but you can use implementation based on [Http4s client](https://github.com/http4s/http4s):

```sbt
libraryDependencies += "com.github.pawelj-pl" %% "fcm4s-http4s" % "<libraryVersion>"
```

This backend require http4s client instance. If You already created one, You can create backend in following way:

```scala
import cats.effect.IO
import com.github.pawelj_pl.fcm4s.http.HttpBackend
import com.github.pawelj_pl.fcm4s.http4s.Http4sBackend
import org.http4s.client.Client

val client: Client[IO] = ???
val httpBackend: HttpBackend[IO] = Http4sBackend(client)
```

Otherwise You can create instance inside `io.cats.Resource`:

```scala
import cats.effect.IO
import com.github.pawelj_pl.fcm4s.http4s.Http4sBackend

import scala.concurrent.ExecutionContext.global


Http4sBackend.create[IO](global).use(implicit backend => {
    ???
  })
```

## Creating instance manually

Instance of `com.github.pawelj_pl.fcm4s.messaging.Messaging` can be created manually for any type, but You have to provide all implicits. Example instance for `cats.effect.IO`:

```scala
implicit val cs: ContextShift[IO] = IO.contextShift(global)
val messaging: IO[Messaging[IO]] = BlazeClientBuilder[IO](global).resource.use(client => {
  implicit val backend: Http4sBackend[IO] = Http4sBackend(client)
  implicit val clock: Clock[IO] = Clock.create[IO]
  implicit val time: TimeProvider[IO] = TimeProvider.instance[IO]
  implicit val cacheMode: Mode[IO] = scalacache.CatsEffect.modes.async
  implicit val accessTokenAuth: AccessTokenAuth[IO] = AccessTokenAuth.withCache[IO](cfg)
  IO(Messaging.create[IO](cfg))
})
```

## Complex message
It's also possible to create complex as described in [specification](https://firebase.google.com/docs/reference/fcm/rest/v1/projects.messages). Example:

```scala
import java.time.Instant

import com.github.pawelj_pl.fcm4s.messaging.{Destination, FcmOptions, MessageDataEncoder, NotificationMessage}
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
import io.circe.syntax._

import scala.concurrent.duration.Duration


val ExampleAndroidNotification = AndroidNotification(
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

val ExampleAndroidConfig = Android(
  collapseKey = Some("someKey"),
  priority = Some(MessagePriority.High),
  ttl = Some(Duration(2200, "millisecond")),
  restrictedPackageName = Some("some.package"),
  data = Some(Map("bazQux" -> "quux", "aB" -> "C")),
  notification = Some(ExampleAndroidNotification),
  fcmOptions = Some(FcmOptions(analyticsLabel = Some("someLabel")))
)

val ExampleWebpushNotification = WebpushNotification(
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

val ExampleWebpushConfig = Webpush(
  headers = Some(Map("h1" -> "v1", "h2" -> "v2")),
  data = Some(Map("key1" -> "value1", "key2" -> "value2")),
  notification = Some(ExampleWebpushNotification),
  fcmOptions = Some(WebpushFcmOptions(link = Some("someLink"), analyticsLabel = Some("someLabel")))
)

val ExampleApnsPayladAlert = ApnsPayloadAlert.AlertDict(
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

val ExampleApnsPayload = StandardApnsPayload(
  alert = Some(ExampleApnsPayladAlert),
  badge = Some(3),
  sound = Some(ApnsPayloadSound.SoundDict(critical = 1, name = "someSound", volume = 1.0)),
  threadId = Some("someThread"),
  category = Some("someCategory"),
  contentAvailable = Some(1),
  mutableContent = Some(1),
  targetContentId = Some("someTargetContentId")
)

val ExampleApnsConfig = Apns.withStandardPayload(
  headers = Some(Map("h1" -> "v1", "h2" -> "v2")),
  payload = ExampleApnsPayload,
  fcmOptions = Some(ApnsFcmOptions(analyticsLabel = Some("someLabel"), image = Some("someImage")))
)

case class TestMessageData(foo: String, bar: Int)
implicit val dataEncoder: MessageDataEncoder[TestMessageData] =
  MessageDataEncoder.deriveFrom(data => Map("foo" -> data.foo, "bar" -> data.bar.toString))

val message = NotificationMessage(
  destination = Destination.Token("someToken"),
  title = Some("someTitle"),
  body = Some("someBody"),
  image = Some("someImage"),
  data = Some(TestMessageData("baz", 8)),
  name = Some("someMessageName"),
  android = Some(ExampleAndroidConfig),
  webpush = Some(ExampleWebpushConfig),
  apns = Some(ExampleApnsConfig),
  fcmOptions = Some(FcmOptions(analyticsLabel = Some("someLabel")))
)
```

will be transformed into following FCM message:

```json
{
  "message" : {
    "name" : "someMessageName",
    "android" : {
      "collapse_key" : "someKey",
      "priority" : "high",
      "ttl" : "2.2s",
      "restricted_package_name" : "some.package",
      "data" : {
        "bazQux" : "quux",
        "aB" : "C"
      },
      "notification" : {
        "title" : "Android title",
        "body" : "Android body",
        "icon" : "Android icon",
        "color" : "Android color",
        "sound" : "Android sound",
        "tag" : "Android tag",
        "click_action" : "Android click action",
        "body_loc_key" : "Android body loc key",
        "title_loc_key" : "Android title loc key",
        "title_loc_args" : [
          "foo",
          "bar"
        ],
        "channel_id" : "Android channel Id",
        "ticker" : "Android ticker",
        "sticky" : true,
        "event_time" : "1970-01-01T00:00:00.000123456Z",
        "local_only" : true,
        "notification_priority" : "PRIORITY_MAX",
        "default_sound" : true,
        "default_vibrate_timings" : true,
        "default_light_settings" : true,
        "vibrate_timings" : [
          "3s",
          "0.000120123s"
        ],
        "visibility" : "VISIBILITY_UNSPECIFIED",
        "notification_count" : 7,
        "light_settings" : {
          "color" : {
            "red" : 0.3,
            "green" : 0.2,
            "blue" : 0.1,
            "alpha" : 1.0
          },
          "light_on_duration" : "0.3s",
          "light_off_duration" : "10s"
        },
        "image" : "Android image"
      },
      "fcm_options" : {
        "analytics_label" : "someLabel"
      }
    },
    "webpush" : {
      "headers" : {
        "h1" : "v1",
        "h2" : "v2"
      },
      "data" : {
        "key1" : "value1",
        "key2" : "value2"
      },
      "notification" : {
        "actions" : {
          "foo" : "bar",
          "baz" : "qux"
        },
        "badge" : "someBadge",
        "body" : "someBody",
        "data" : {
          "a" : "b",
          "c" : "d"
        },
        "dir" : "ltr",
        "lang" : "pl-PL",
        "tag" : "someTag",
        "icon" : "someIcon",
        "image" : "someImage",
        "renotify" : true,
        "requireInteraction" : true,
        "silent" : true,
        "timestamp" : 1589227678407,
        "title" : "someTitle",
        "vibrate" : [
          300,
          200,
          300
        ]
      },
      "fcm_options" : {
        "link" : "someLink",
        "analytics_label" : "someLabel"
      }
    },
    "apns" : {
      "headers" : {
        "h1" : "v1",
        "h2" : "v2"
      },
      "payload" : {
        "aps" : {
          "alert" : {
            "title" : "someTitle",
            "subtitle" : "someSubtitle",
            "body" : "someBody",
            "launch-image" : "someLaunchImage",
            "title-loc-key" : "someTitleLocKey",
            "title-loc-args" : [
              "titleLoc1",
              "titleLoc2"
            ],
            "subtitle-loc-key" : "someSubtitleLocKey",
            "subtitle-loc-args" : [
              "subtitleLoc1",
              "subtitleLoc2"
            ],
            "loc-key" : "someLocKey",
            "loc-args" : [
              "loc1",
              "loc2"
            ]
          },
          "badge" : 3,
          "sound" : {
            "critical" : 1,
            "name" : "someSound",
            "volume" : 1.0
          },
          "thread-id" : "someThread",
          "category" : "someCategory",
          "content-available" : 1,
          "mutable-content" : 1,
          "target-content-id" : "someTargetContentId"
        }
      },
      "fcm_options" : {
        "analytics_label" : "someLabel",
        "image" : "someImage"
      }
    },
    "fcm_options" : {
      "analytics_label" : "someLabel"
    },
    "data" : {
      "foo" : "baz",
      "bar" : "8"
    },
    "notification" : {
      "title" : "someTitle",
      "body" : "someBody",
      "image" : "someImage"
    },
    "token" : "someToken"
  }
}
```