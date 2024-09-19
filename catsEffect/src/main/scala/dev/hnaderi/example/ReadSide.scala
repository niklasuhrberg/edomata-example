package dev.hnaderi.example

import cats.effect.Async
import cats.effect.kernel.Concurrent
import cats.effect.std.Console
import fs2.Stream
import fs2.io.net.Network
import natchez.Trace.Implicits.noop
import skunk.Session
object ReadSide {
  def run[F[_]: Async: Console: Network: Concurrent]: F[Unit] = {
    (for {
      app <- Stream.resource(Application[F]())
      pool <- Stream.resource(Session.pooled[F](
        host = "localhost",
        user = "postgres",
        password = Some("postgres"),
        database = "metadata_read",
        max = 10
      ))
      processor <- Stream.eval(SkunkReadModelOps.make[F](pool))
      result <- app.metadataApp.storage.journal.readAll.foreach(e => processor.process(e))
    } yield()).compile.drain
  }
}
