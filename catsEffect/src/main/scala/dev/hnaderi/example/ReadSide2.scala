package dev.hnaderi.example

import cats.effect.IO

object ReadSide2 {
  def run: IO[Unit] = Application[IO]().use(app =>
    import fs2.Stream.*
    import scala.concurrent.duration.*

    val consumer = for {
      offset <- eval(IO.ref(-1l))
      _ <- app.metadataApp.storage.updates.journal
      current <- eval(offset.get)
      event <- app.metadataApp.storage.journal.readAllAfter(current)
      _ <- eval(IO.println(s"Consumer: $event"))
      _ <- exec(offset.set(event.metadata.seqNr))
    } yield ()

    val consumerDistributed = for {
      offset <- eval(IO.ref(-1l))
      _ <- awakeEvery[IO](10.seconds)
      current <- eval(offset.get)
      event <- app.metadataApp.storage.journal.readAllAfter(current)
      _ <- eval(IO.println(s"ConsumerDistributed: $event"))
      _ <- exec(offset.set(event.metadata.seqNr))
    } yield ()

    consumer.concurrently(consumerDistributed).compile.drain
  )
}
