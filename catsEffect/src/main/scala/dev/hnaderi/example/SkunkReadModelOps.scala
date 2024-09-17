package dev.hnaderi.example
import cats.{Applicative, Monad}
import cats.syntax.all.*
import cats.effect.std.Console
import cats.effect.{Async, Concurrent, Resource}
import dev.hnaderi.example.metadata.Event
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
  (uuid, uuid, uuid, varchar, varchar).tupled.imap {
    case (id, entityId, parent, createdBy, category) => InsertMetadataRow(id, entityId, Option(parent),createdBy, category)
  } { inr => (inr.id, inr.entityId, inr.parent.fold[UUID](UUID.randomUUID())(identity), inr.createdBy, inr.category) }

def insertCommand: Command[InsertMetadataRow] =
  sql"""
    INSERT INTO metadata VALUES($codec)
    """.command

final case class SkunkReadModelOps[F[_]:Monad: Concurrent: Console](pool: Resource[F,Session[F]]) extends ReadModelOps[F] {
  override def process(event: Event): F[Unit] = {
    val ini: InsertMetadataRow = event match {
      case Event.Created(entityId, parent, category) => InsertMetadataRow(UUID.randomUUID(), entityId,
        None, "default", category)
    }
    pool.use(s => for {
      command <- s.prepare(insertCommand)
      rowCount <- command.execute(ini)
      _ <- Console[F].println(s"Executed insert with rowcount $rowCount")
    } yield ())
    //Console[F].println(event)
  }
}

object SkunkReadModelOps {
  def apply[F[_]:Async: Console](pool: Resource[F, Session[F]]): SkunkReadModelOps[F] =
    new SkunkReadModelOps[F](pool)
}
