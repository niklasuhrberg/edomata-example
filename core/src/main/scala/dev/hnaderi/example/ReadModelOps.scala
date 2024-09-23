package dev.hnaderi.example

import edomata.backend.EventMessage

trait ReadModelOps[F[_], T] {

  def process(event: EventMessage[T]):F[Unit]
}
