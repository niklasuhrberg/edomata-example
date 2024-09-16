package dev.hnaderi.example

import cats.effect.Async
import cats.effect.std.Console
import dev.hnaderi.example.metadata.Event
final case class PrintReadModelOps[F[_]:Console](s: String) extends ReadModelOps[F] {
  override def process(event: Event): F[Unit] = Console[F].println(event)
}

object PrintReadModelOps {
  def apply[F[_] :Console](): PrintReadModelOps[F] = {
    PrintReadModelOps[F]("s")
  }
}
