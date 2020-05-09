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

val publishSettings = List(
  useJGit,
  sonatypeBundleDirectory := (ThisBuild / baseDirectory).value / target.value.getName / "sonatype-staging" / s"${version.value}",
  publishTo := sonatypePublishToBundle.value,
  publishMavenStyle := true,
  developers := List(
    Developer(
      id    = "Pawelj-PL",
      name  = "Pawel",
      email = "inne.poczta@gmail.com",
      url   = url("https://github.com/PawelJ-PL")
    )
  ),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/PawelJ-PL/fcm4s"),
      "scm:https://github.com/PawelJ-PL/fcm4s.git"
    )
  ),
  pomIncludeRepository := { _ => false },
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/PawelJ-PL/fcm4s"))
)

val commonSettings = Seq(
  organization := "com.github.pawelj-pl",
  name := "fcm4s",
  scalaVersion := "2.13.0",
  crossScalaVersions := Seq("2.13.0", "2.12.8"),
  scalacOptions ++= commonScalacOptions ++ versionDependentScalacOptions(scalaVersion.value)
)

val core = (project in file("modules/core"))
  .settings(publishSettings)
  .settings(commonSettings)
  .settings(
    name += "-core",
    libraryDependencies ++= {
      val cats = Seq(
        "org.typelevel" %% "cats-effect" % "2.1.3"
      )

      val circe = Seq(
        "io.circe" %% "circe-parser",
        "io.circe" %% "circe-generic",
        "io.circe" %% "circe-generic-extras",
      ).map(_ % "0.13.0")

      val circeDerivation = Seq(
        "io.circe" %% "circe-derivation" % "0.13.0-M4"
      )

      val http4s = Seq(
        "org.http4s" %% "http4s-core",
        "org.http4s" %% "http4s-circe"
      ).map(_ % "0.21.4")

      val tsec = Seq(
        "io.github.jmcardon" %% "tsec-jwt-sig",
        "io.github.jmcardon" %% "tsec-signatures"
      ).map(_ % "0.2.0")

      val scalaCache = Seq(
        "com.github.cb372" %% "scalacache-core",
        "com.github.cb372" %% "scalacache-cats-effect",
        "com.github.cb372" %% "scalacache-caffeine"
      ).map(_ % "0.28.0")

      val tests = Seq(
        "org.scalatest" %% "scalatest" % "3.0.8",
        "io.circe" %% "circe-literal" % "0.13.0"
      ).map(_ % Test)

      circe ++ circeDerivation ++ cats ++ http4s ++ tsec ++ scalaCache ++ tests ++ versionDependentDependencies(scalaVersion.value)
    }
  )

val http4s = (project in file("modules/http4s"))
  .settings(publishSettings)
  .settings(commonSettings)
  .settings(
    name += "-http4s",
    libraryDependencies ++= {
      val http4s = Seq(
        "org.http4s" %% "http4s-blaze-client",
      ).map(_ % "0.21.4")

      http4s
    }
  )
  .dependsOn(core)

val root = (project in file("."))
  .settings(publishSettings)
  .settings(commonSettings)
  .settings(
    publishArtifact := false,
    publish := {},
    publishLocal := {},
  )
  .enablePlugins(GitVersioning)
  .aggregate(core, http4s)
  .dependsOn(core, http4s)
