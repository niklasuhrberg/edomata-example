package dev.hnaderi.example

import cats.effect.Async
import cats.effect.std.Console
import cats.syntax.*
import cats.syntax.all.*
import fs2.io.net.Network

object StreamLab2 {

  def run[F[_] : Async : Network : Console]: F[Unit] = {
    val result = for {
      a <- fs2.Stream.eval[F, Int](1.pure)
      b <- fs2.Stream.eval[F, Int](1.pure).printlns
    } yield()

    result.compile.drain
  }
}
