package com.github.pawelj_pl.fcm4s.utils

import java.time.Instant

import cats.Functor
import cats.effect.Clock
import cats.syntax.functor._

import scala.concurrent.duration.MILLISECONDS

trait TimeProvider[F[_]] {
  def instant: F[Instant]
}

object TimeProvider {
  def apply[F[_]](implicit ev: TimeProvider[F]): TimeProvider[F] = ev
  def instance[F[_]: Functor: Clock]: TimeProvider[F] = new TimeProvider[F] {
    override def instant: F[Instant] = Clock[F].realTime(MILLISECONDS).map(Instant.ofEpochMilli)
  }
}