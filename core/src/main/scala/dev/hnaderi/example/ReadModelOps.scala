package dev.hnaderi.example

import dev.hnaderi.example.metadata.Event
import edomata.backend.EventMessage

trait ReadModelOps[F[_], T] {

  def process(event: EventMessage[T]):F[Unit]
}
