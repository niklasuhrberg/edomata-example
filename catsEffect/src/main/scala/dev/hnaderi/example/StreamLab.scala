package dev.hnaderi.example

import cats.effect.Async
import cats.effect.std.Console
import cats.syntax.*
import cats.instances.*
import fs2.io.net.Network
import cats.syntax.all.*

object StreamLab {

  def run[F[_] : Async : Network : Console]: F[Unit] = {
    fs2.Stream.eval[F, Int](1.pure).printlns.compile.drain
  }
}
