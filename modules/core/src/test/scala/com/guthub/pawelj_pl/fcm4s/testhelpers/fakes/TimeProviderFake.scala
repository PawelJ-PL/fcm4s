package com.guthub.pawelj_pl.fcm4s.testhelpers.fakes

import java.time.Instant

import cats.Applicative
import com.github.pawelj_pl.fcm4s.utils.TimeProvider

class TimeProviderFake[F[_]: Applicative](now: Instant) extends TimeProvider[F] {
  override def instant: F[Instant] = Applicative[F].pure(now)
}
