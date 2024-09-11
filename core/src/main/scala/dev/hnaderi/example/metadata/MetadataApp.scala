package dev.hnaderi.example.metadata

import edomata.skunk.{BackendCodec, CirceCodec, SkunkDriver}
import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import edomata.backend.Backend
import edomata.backend.eventsourcing
import io.circe.generic.auto.*
import skunk.Session

object MetadataApp {
  given BackendCodec[Event] = CirceCodec.jsonb

  given BackendCodec[Notification] = CirceCodec.jsonb

  given BackendCodec[Metadata] = CirceCodec.jsonb

  def backend[F[_] : Async](pool: Resource[F, Session[F]]) = Backend
    .builder(MetadataService)
    .use(SkunkDriver("eventsourcing_example", pool))
    //.disableCache
    .persistedSnapshot(maxInMem = 3, maxBuffer = 3)
    .build

  def apply[F[_] : Async](pool: Resource[F, Session[F]]) =
    backend(pool).map(s => new MetadataApp(s, s.compile(MetadataService[F])))
}

final case class  MetadataApp[F[_]](
                                    storage: eventsourcing.Backend[F, Metadata, Event, Rejection, Notification],
                                    service: MetadataService.Handler[F]
                                  )
