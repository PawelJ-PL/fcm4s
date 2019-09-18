package com.guthub.pawelj_pl.fcm4s.testhelpers.fakes

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.github.pawelj_pl.fcm4s.http.HttpBackend
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._
import org.http4s.Uri

trait HttpBackendFake[F[_]] extends HttpBackend[F] {
  def getCalls: F[Seq[(Uri, Option[Json])]]
}

object HttpBackendFake {
  case class BackendState(responses: Seq[Either[(Int, String), Json]] = Seq.empty)

  def instanceF[F[_]: Sync](state: BackendState): F[HttpBackendFake[F]] =
    for {
      calls     <- Ref.of[F, Seq[(Uri, Option[Json])]](Seq.empty)
      responses <- Ref.of[F, BackendState](state)
    } yield createInstance(calls, responses)

  private def createInstance[F[_]: Sync](calls: Ref[F, Seq[(Uri, Option[Json])]], responses: Ref[F, BackendState]): HttpBackendFake[F] =
    new HttpBackendFake[F] {

      override def sendPostForm[A: Decoder](uri: Uri, formData: Map[String, String]): F[Either[(Int, String), A]] =
        processCall[A](uri, Some(formData.asJson))

      private def processCall[A: Decoder](uri: Uri, payload: Option[Json]): F[Either[(Int, String), A]] =
        for {
          _ <- calls.update(old => old :+ ((uri, payload)))
          resp <- responses.modify[Either[(Int, String), Json]](state => {
            state.responses match {
              case head :: _ => (BackendState(state.responses.drop(1)), head)
              case _         => (state, (404, "No matching response found").asLeft[Json])
            }
          })
        } yield
          resp.flatMap(
            body =>
              Decoder[A]
                .decodeJson(body)
                .leftMap(decodingFailure => (400, s"unable to decode body: $body, error: ${decodingFailure.message}")))

      override def sendPost[A: Encoder, B: Decoder](uri: Uri, payload: A, bearerToken: String): F[Either[(Int, String), B]] =
        processCall[B](uri, Some(payload.asJson))

      override def getCalls: F[Seq[(Uri, Option[Json])]] = calls.get
    }
}
