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
//  def run[F[_]: Async: Console: Network: Concurrent]: F[Unit] = {
//    val run0 = for {
//      app <- Stream.eval(Application[F]())
//      result <- app.metadataApp.storage.journal.readAll.printlns()
//    } yield ()
//    run0.compile.drain
//    ().pure
//  }
}
