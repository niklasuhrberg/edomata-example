package dev.hnaderi.example

import cats.effect.std.Console
import dev.hnaderi.example.metadata.Event
import edomata.backend.EventMessage
final case class PrintReadModelOps[F[_]:Console](s: String) extends ReadModelOps[F, Event] {
  override def process(event: EventMessage[Event]): F[Unit] = Console[F].println(event.payload)
}

object PrintReadModelOps {
  def apply[F[_] :Console](): PrintReadModelOps[F] = {
    PrintReadModelOps[F]("s")
  }
}
