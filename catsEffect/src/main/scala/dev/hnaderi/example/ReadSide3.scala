package dev.hnaderi.example

import cats.Applicative
import cats.effect.Async
import cats.effect.kernel.Concurrent
import cats.effect.std.Console
import edomata.backend.EventMessage
import fs2.Stream
import fs2.io.net.Network
import natchez.Trace.Implicits.noop
import skunk.Session
import dev.hnaderi.example.metadata.Event

object ReadSide3 {
  def run[F[_]: Async: Console: Network: Concurrent]: F[Unit] = {
    import fs2.Stream.*

    import scala.concurrent.duration.*
    val partition = 4
    def partitionedProcess[F[_]: Async: Concurrent](event:EventMessage[Event], processor: SkunkReadModelOps[F]) = {
      val par = Math.abs(event.metadata.stream.hashCode()) % 5
      if(par == partition) {
        processor.process(event)
      } else {
        Concurrent[F].pure(())
      }
    }

    val consumer = for {
      app <- Stream.resource(Application[F]())
      pool <- Stream.resource(Session.pooled[F](
        host = "localhost",
        user = "postgres",
        password = Some("postgres"),
        database = "metadata_read",
        max = 10
      ))
      processor <- Stream.eval(SkunkReadModelOps.make[F](pool))
      offset <- Stream.eval(Concurrent[F].ref[Long](800000))
      _ <- awakeEvery[F](500.millis)
      current <- eval(offset.get)
      event <- app.metadataApp.storage.journal.readAllAfter(current)//.foreach(e => processor.process(e))
      //_ <- eval(partitionedProcess(event, processor))
      _ <- eval(processor.process(event))
      _ <- eval(Console[F].println(s"Consumer: $event"))
      _ <- exec(offset.set(event.metadata.seqNr))
    } yield()

      consumer.compile.drain
  }
}
