package dev.hnaderi.example

import cats.effect.kernel.Concurrent
import cats.effect.std.Console
import cats.effect.{Async, IO, Resource}
import cats.implicits.*
import cats.syntax.all.*
import dev.hnaderi.example.metadata.Event
import edomata.backend.EventMessage
import fs2.Stream
import fs2.io.net.Network
import natchez.Trace.Implicits.noop
import skunk.Session
object ReadSide {
  def run[F[_]: Async: Console: Network: Concurrent]: F[Unit] = {
    val run0 = for {
      app <- Stream.resource(Application[F]())
      pool <- Stream.resource(Session.pooled[F](
        host = "localhost",
        user = "postgres",
        password = Some("postgres"),
        database = "metadata_read",
        max = 10
      ))

      processor = new SkunkReadModelOps[F](pool)
      result <- app.metadataApp.storage.journal.readAll.foreach(e => processor.process(e.payload))
    } yield ()
    run0.compile.drain
    //().pure
  }
}
