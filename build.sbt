val commonScalacOptions = Seq(
  "-feature",
  "-language:higherKinds",
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-unchecked",
  "-explaintypes",
  "-Xfatal-warnings",
  "-Xlint:infer-any",
  "-Xlint:private-shadow",
  "-Xlint:missing-interpolator",
  "-Ywarn-dead-code",
  "-Ywarn-unused",
  "-Ywarn-unused:privates",
)

def versionDependentScalacOptions(scalaVersion: String): Seq[String] = CrossVersion.partialVersion(scalaVersion) match {
  case Some((2, scalaMinor)) if scalaMinor == 12 => Seq(
    "-Ypartial-unification",
    "-Ywarn-unused-import",
    "-Xlint:unsound-match",
    "-Ywarn-infer-any"
  )
  case Some((2, scalaMinor)) if scalaMinor == 13 => Seq(
    "-Ymacro-annotations"
  )
}

def versionDependentDependencies(scalaVersion: String) = CrossVersion.partialVersion(scalaVersion) match {
  case Some((2, scalaMinor)) if scalaMinor == 12 => Seq(
    compilerPlugin(("org.scalamacros" % "paradise" % "2.1.1").cross(CrossVersion.full))
  )
  case _                                         => Seq()
}

def circeVersion(scalaVersion: String): String = CrossVersion.partialVersion(scalaVersion) match {
  case Some((2, scalaMinor)) if scalaMinor == 12 => "0.11.1"
  case _                                         => "0.12.1"
}

def catsEffectVersion(scalaVersion: String): String = CrossVersion.partialVersion(scalaVersion) match {
  case Some((2, scalaMinor)) if scalaMinor == 12 => "1.4.0"
  case _                                         => "2.0.0"
}

def http4sVersion(scalaVersion: String): String = CrossVersion.partialVersion(scalaVersion) match {
  case Some((2, scalaMinor)) if scalaMinor == 12 => "0.20.10"
  case _                                         => "0.21.0-M5"
}

def tsecVersion(scalaVersion: String): String = CrossVersion.partialVersion(scalaVersion) match {
  case Some((2, scalaMinor)) if scalaMinor == 12 => "0.1.0"
  case _                                         => "0.2.0-M1"
}

val scalaCacheVersion = "0.28.0"

val commonSettings = Seq(
  organization := "com.github.pawelj-pl",
  name := "fcm4s",
  scalaVersion := "2.13.0",
  crossScalaVersions := Seq("2.13.0", "2.12.8"),
  scalacOptions ++= commonScalacOptions ++ versionDependentScalacOptions(scalaVersion.value),
  useJGit,
)

val core = (project in file("modules/core"))
  .settings(commonSettings)
  .settings(
    name += "-core",
    libraryDependencies ++= {
      val cats = Seq(
        "org.typelevel" %% "cats-effect" % catsEffectVersion(scalaVersion.value)
      )

      val circe = Seq(
        "io.circe" %% "circe-parser",
        "io.circe" %% "circe-generic",
        "io.circe" %% "circe-generic-extras",
      ).map(_ % circeVersion(scalaVersion.value))

      val http4s = Seq(
        "org.http4s" %% "http4s-core",
        "org.http4s" %% "http4s-circe"
      ).map(_ % http4sVersion(scalaVersion.value))

      val tsec = Seq(
        "io.github.jmcardon" %% "tsec-jwt-sig",
        "io.github.jmcardon" %% "tsec-signatures"
      ).map(_ % tsecVersion(scalaVersion.value))

      val scalaCache = Seq(
        "com.github.cb372" %% "scalacache-core",
        "com.github.cb372" %% "scalacache-cats-effect",
        "com.github.cb372" %% "scalacache-caffeine"
      ).map(_ % scalaCacheVersion)

      val tests = Seq(
        "org.scalatest" %% "scalatest" % "3.0.8",
        "io.circe" %% "circe-literal" % circeVersion((scalaVersion.value))
      ).map(_ % Test)

      circe ++ cats ++ http4s ++ tsec ++ scalaCache ++ tests ++ versionDependentDependencies(scalaVersion.value)
    }
  )

val http4s = (project in file("modules/http4s"))
  .settings(commonSettings)
  .settings(
    name += "-http4s",
    libraryDependencies ++= {
      val http4s = Seq(
        "org.http4s" %% "http4s-blaze-client",
      ).map(_ % http4sVersion(scalaVersion.value))

      http4s
    }
  )
  .dependsOn(core)

val root = (project in file("."))
  .settings(commonSettings)
  .settings(
    publishArtifact := false,
    publish := {},
    publishLocal := {}
  )
  .enablePlugins(GitVersioning)
  .aggregate(core, http4s)
  .dependsOn(core, http4s)
