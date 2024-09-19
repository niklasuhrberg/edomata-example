package dev.hnaderi.example
import cats.{Applicative, Monad}
import cats.syntax.all.*
import cats.effect.std.Console
import cats.effect.{Async, Concurrent, Resource, Sync}
import dev.hnaderi.example.metadata.Event
import dev.hnaderi.example.metadata.Event.Created
import edomata.backend.EventMessage
import skunk.{Codec, Command, Session}
import skunk.implicits.sql
import skunk.*
import skunk.codec.all.*
import skunk.syntax.all.*
import java.time.{Instant, OffsetDateTime, ZoneId}
import java.util.UUID
import dev.hnaderi.example.metadata.MetadataItem

case class InsertMetadataRow( id: UUID,
entityId: UUID ,
parent: Option[UUID],
createdBy: String,
category: String)

case class InsertAttachmentRow(
                              id:UUID,
                              status: String,
                              name: String,
                              fileExtension: String,
                              description: String,
                              contentType: String,
                              location: String,
                              version: Int,
                              createdAt: Instant
                              )

//id uuid
//status varchar
//name varchar
//fileExtension varchar
//description varchar
//content_type varchar
//location varchar
//version integer DEFAULT 0
//created_at timestamptz not DEFAULT CURRENT_TIMESTAMP

val attachemtCodec: Codec[InsertAttachmentRow] =
  (uuid, varchar, varchar, varchar, varchar, varchar, varchar, int4, timestamptz).tupled.imap {
    case (id, status, name, fileExtension, description, contentType, location, version, createdAt) =>
      InsertAttachmentRow(id, status, name, fileExtension, description, contentType, location, version, createdAt.toInstant)
  } {
    a => (a.id, a.status, a.name, a.fileExtension, a.description, a.contentType, a.location, a.version, OffsetDateTime.ofInstant(a.createdAt, ZoneId.systemDefault()))
  }

val codec: Codec[InsertMetadataRow] =
  (uuid, uuid, uuid.opt, varchar, varchar).tupled.imap {
    case (id, entityId, parent, createdBy, category) => InsertMetadataRow(id, entityId, parent,createdBy, category)
  } { inr => (inr.id, inr.entityId, inr.parent, inr.createdBy, inr.category) }

def insertCommand: Command[InsertMetadataRow] =
  sql"""
    INSERT INTO metadata VALUES($codec)
    """.command

def insertAttachmentCommand: Command[InsertAttachmentRow] =
  sql"""
    INSERT INTO attachments VALUES($attachemtCodec)
    """.command
def filterItem(items: List[MetadataItem], arg: String):String = items.find(_.name == arg).map(_.value).get
def itemsToAttachment(eventMessage: EventMessage[Event]): InsertAttachmentRow = {
  val c = eventMessage.payload.asInstanceOf[Event.Created]
  InsertAttachmentRow(
    id = UUID.fromString(eventMessage.metadata.stream),
    status = filterItem(c.items, "status"),
    name = filterItem(c.items, "name"),
    fileExtension = filterItem(c.items, "fileExtension"),
    description = filterItem(c.items, "description"),
    contentType = filterItem(c.items, "contentType"),
    location = filterItem(c.items, "location"),
    version = 0,
    createdAt = Instant.now()
  )
}
def itemsToMetadata(eventMessage: EventMessage[Event]): InsertMetadataRow = {
  val c = eventMessage.payload.asInstanceOf[Event.Created]
  InsertMetadataRow(UUID.fromString(eventMessage.metadata.stream), c.entityId,
    c.parent, c.user, c.category)
}

  def processAttachment[F[_]:Monad: Console](s: Session[F], event: EventMessage[Event]): F[Unit] = for {
  command <- s.prepare(insertAttachmentCommand)
  rowCount <- command.execute(itemsToAttachment(event))
} yield ()
def processMetadata[F[_] : Monad : Console](s: Session[F], event: EventMessage[Event]): F[Unit] = for {
  command <- s.prepare(insertCommand)
  rowCount <- command.execute(itemsToMetadata(event))
} yield ()


final case class SkunkReadModelOps[F[_]:Monad: Concurrent: Console](pool: Resource[F,Session[F]]) extends ReadModelOps[F, Event] {
  override def process(event: EventMessage[Event]): F[Unit] = {
    val a : InsertAttachmentRow = event.payload match {
      case Event.Created(entityId, parent, category, user, items) => itemsToAttachment(event)
    }
//    val ini: InsertMetadataRow = event.payload match {
//      case Event.Created(entityId, parent, category, user, _) => InsertMetadataRow(UUID.fromString(event.metadata.stream), entityId,
//        parent, user, category)
//    }
    pool.use(s => for {
      command <- s.prepare(insertAttachmentCommand)
      rowCount <- command.execute(a)
      _ <- Console[F].println(s"Executed insert with rowcount $rowCount")
    } yield ())
  }
}

object SkunkReadModelOps {
  def make[F[_]:Async: Sync: Console](pool: Resource[F, Session[F]]): F[SkunkReadModelOps[F]] =
    Sync[F].delay(new SkunkReadModelOps[F](pool))
}
