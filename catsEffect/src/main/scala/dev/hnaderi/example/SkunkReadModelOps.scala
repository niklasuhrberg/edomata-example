package dev.hnaderi.example
import cats.{Applicative, Monad}
import cats.syntax.all.*
import cats.effect.std.Console
import cats.effect.{Async, Concurrent, Resource, Sync}
import dev.hnaderi.example.metadata.Event
import edomata.backend.EventMessage
import skunk.{Codec, Command, Session}
import skunk.implicits.sql
import skunk.*
import skunk.codec.all.*
import skunk.syntax.all.*
import fs2.Compiler.Target.forConcurrent

import java.util.UUID

case class InsertMetadataRow( id: UUID,
entityId: UUID ,
parent: Option[UUID],
createdBy: String,
category: String)

val codec: Codec[InsertMetadataRow] =
  (uuid, uuid, uuid.opt, varchar, varchar).tupled.imap {
    case (id, entityId, parent, createdBy, category) => InsertMetadataRow(id, entityId, parent,createdBy, category)
  } { inr => (inr.id, inr.entityId, inr.parent, inr.createdBy, inr.category) }

def insertCommand: Command[InsertMetadataRow] =
  sql"""
    INSERT INTO metadata VALUES($codec)
    """.command

final case class SkunkReadModelOps[F[_]:Monad: Concurrent: Console](pool: Resource[F,Session[F]]) extends ReadModelOps[F, Event] {
  override def process(event: EventMessage[Event]): F[Unit] = {

    val ini: InsertMetadataRow = event.payload match {
      case Event.Created(entityId, parent, category, user, _) => InsertMetadataRow(UUID.fromString(event.metadata.stream), entityId,
        parent, user, category)
    }
    pool.use(s => for {
      command <- s.prepare(insertCommand)
      rowCount <- command.execute(ini)
      _ <- Console[F].println(s"Executed insert with rowcount $rowCount")
    } yield ())
  }
}

object SkunkReadModelOps {
  def make[F[_]:Async: Sync: Console](pool: Resource[F, Session[F]]): F[SkunkReadModelOps[F]] =
    Sync[F].delay(new SkunkReadModelOps[F](pool))
}
