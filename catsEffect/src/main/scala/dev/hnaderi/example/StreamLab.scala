package dev.hnaderi.example

import cats.effect.Async
import cats.effect.std.Console
import cats.syntax.*
import cats.syntax.all.*
import fs2.io.net.Network

object StreamLab {

  def run[F[_] : Async : Network : Console]: F[Unit] = {
    fs2.Stream.eval[F, Int](1.pure).printlns.compile.drain
  }
}
