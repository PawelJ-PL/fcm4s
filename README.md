# Fcm4s

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