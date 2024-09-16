package dev.hnaderi.example

import cats.effect.kernel.Concurrent
import cats.effect.{Async, IO, Resource}
import cats.effect.std.Console
import fs2.io.net.Network
import fs2.Stream
import cats.syntax.all.*
import cats.implicits.*
import dev.hnaderi.example.metadata.Event
import edomata.backend.EventMessage
object ReadSide {
  def run[F[_]: Async: Console: Network: Concurrent]: F[Unit] = {
    val run0 = for {
      app <- Stream.resource(Application[F]())
      processor = PrintReadModelOps[F]()
      result <- app.metadataApp.storage.journal.readAll.foreach(e => processor.process(e.payload))
    } yield ()
    run0.compile.drain
    //().pure
  }
}
