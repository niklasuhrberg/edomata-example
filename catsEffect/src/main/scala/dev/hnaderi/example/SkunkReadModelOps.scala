package dev.hnaderi.example

import cats.syntax.all.catsSyntaxApplicativeId
import cats.effect.{Async, Resource}
import cats.effect.std.Console
import dev.hnaderi.example.metadata.Event
import skunk.Session
final case class SkunkReadModelOps[F[_]:Console](pool: Resource[F,Session[F]]) extends ReadModelOps[F] {
  override def process(event: Event): F[Unit] = {
    Console[F].println(event)
  }
}

object SkunkReadModelOps {
  def apply[F[_]:Async: Console](pool: Resource[F, Session[F]]): SkunkReadModelOps[F] =
    new SkunkReadModelOps[F](pool)
}
