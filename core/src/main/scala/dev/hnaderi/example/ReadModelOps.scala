package dev.hnaderi.example

trait ReadModelOps[F[_]] {

  def process(event: metadata.Event):F[Unit]
}
