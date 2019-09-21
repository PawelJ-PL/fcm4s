package com.guthub.pawelj_pl.fcm4s.testhelpers.fakes

import cats.Functor
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.functor._
import com.github.pawelj_pl.fcm4s.auth.{AccessTokenAuth, AccessTokenResponse}

trait AccessTokenAuthFake[F[_]] extends AccessTokenAuth[F] {
  def getCalls: F[Seq[AuthCall]]
}

object AccessTokenAuthFake {
  def instanceF[F[_]: Sync]: F[AccessTokenAuthFake[F]] = Ref.of[F, Seq[AuthCall]](Seq.empty).map(createInstance[F])

  private def createInstance[F[_]: Functor](calls: Ref[F, Seq[AuthCall]]): AccessTokenAuthFake[F] = new AccessTokenAuthFake[F] {
    override def getCalls: F[Seq[AuthCall]] = calls.get

    override def token: F[String] = calls.update(old => old :+ AuthCall.Token).as("someToken")

    override def refresh: F[AccessTokenResponse] = calls.update(old => old :+ AuthCall.Refresh).as(AccessTokenResponse("someToken", 10, "bearer"))
  }
}

sealed trait AuthCall extends Product with Serializable

case object AuthCall {
  final case object Token extends AuthCall
  final case object Refresh extends AuthCall
}