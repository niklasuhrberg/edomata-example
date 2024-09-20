package dev.hnaderi.example
import cats.Monad
import cats.effect.std.Console
import cats.effect.{Async, Concurrent, Resource, Sync}
import cats.syntax.all.*
import dev.hnaderi.example.metadata.Event.Created
import dev.hnaderi.example.metadata.{Event, MetadataItem}
import edomata.backend.EventMessage
import skunk.codec.all.*
import skunk.implicits.sql
import skunk.syntax.all.*
import skunk.*
import skunk.data.Completion

import java.time.{Instant, OffsetDateTime, ZoneId}
import java.util.UUID

case class InsertMetadataRow(
    id: UUID,
    entityId: UUID,
    parent: Option[UUID],
    createdBy: String,
    category: String
)


case class InsertAttachmentRow(
    id: UUID,
    status: String,
    name: String,
    fileExtension: String,
    description: String,
    contentType: String,
    location: String,
    version: Int,
    createdAt: Instant
)

case class InsertItemRow(
                          id: UUID,
                          metadataId: UUID,
                          name: String,
                          value: String)



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
  (
    uuid,
    varchar,
    varchar,
    varchar,
    varchar,
    varchar,
    varchar,
    int4,
    timestamptz
  ).tupled.imap {
    case (
          id,
          status,
          name,
          fileExtension,
          description,
          contentType,
          location,
          version,
          createdAt
        ) =>
      InsertAttachmentRow(
        id,
        status,
        name,
        fileExtension,
        description,
        contentType,
        location,
        version,
        createdAt.toInstant
      )
  } { a =>
    (
      a.id,
      a.status,
      a.name,
      a.fileExtension,
      a.description,
      a.contentType,
      a.location,
      a.version,
      OffsetDateTime.ofInstant(a.createdAt, ZoneId.systemDefault())
    )
  }

val codec: Codec[InsertMetadataRow] =
  (uuid, uuid, uuid.opt, varchar, varchar).tupled.imap {
    case (id, entityId, parent, createdBy, category) =>
      InsertMetadataRow(id, entityId, parent, createdBy, category)
  } { inr => (inr.id, inr.entityId, inr.parent, inr.createdBy, inr.category) }
val itemCodec: Codec[InsertItemRow] =
  (uuid, uuid, varchar, varchar).tupled.imap {
    case (id, metadataId, name, value) => InsertItemRow(id, metadataId, name, value)
  } { item => (item.id, item.metadataId, item.name, item.value)}

def insertCommand: Command[InsertMetadataRow] =
  sql"""
    INSERT INTO metadata VALUES($codec)
    """.command
def insertItemCommand: Command[InsertItemRow] =
  sql"""
       INSERT INTO items ('id','metadata_id','name','value',) VALUES($itemCodec)
     """.command

def insertAttachmentCommand: Command[InsertAttachmentRow] =
  sql"""
    INSERT INTO attachments VALUES($attachemtCodec)
    """.command
def filterItem(items: List[MetadataItem], arg: String): String =
  items.find(_.name == arg).map(_.value).get
def itemsToAttachment(
    eventMessage: EventMessage[Event]
): InsertAttachmentRow = {
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
  InsertMetadataRow(
    UUID.fromString(eventMessage.metadata.stream),
    c.entityId,
    c.parent,
    c.user,
    c.category
  )
}
def itemToRow(metadataId:UUID, metadataItem: MetadataItem): InsertItemRow = InsertItemRow(
  metadataItem.id,
  metadataId,
  metadataItem.name,
  metadataItem.value
)
def processAttachment[F[_]: Monad: Console](
    s: Session[F],
    event: EventMessage[Event]
): F[Unit] = for {
  command <- s.prepare(insertAttachmentCommand)
  rowCount <- command.execute(itemsToAttachment(event))
} yield ()

def processItem[F[_]:Monad:Console] (
    s:Session[F], item: MetadataItem, metadataId: UUID
                                   ):F[Completion] = for {
    itemCommand <- s.prepare(insertItemCommand)
    r <- itemCommand.execute(itemToRow(metadataId, item))
} yield r
def processMetadata[F[_]: Monad: Console](
    s: Session[F],
    event: EventMessage[Event]
): F[Unit] = for {
  command <- s.prepare(insertCommand)
  rowCount <- command.execute(itemsToMetadata(event))
  r <- event.payload.asInstanceOf[Created].items.traverse(i => processItem[F](s, i,
    UUID.fromString(event.metadata.stream)))
//  itemCommand <- s.prepare(insertItemCommand)
//  r <- event.payload.asInstanceOf[Created].items.map(i =>
//    itemCommand.execute(itemToRow(UUID.fromString(event.metadata.stream), i))).reduce((l,r) => l)
} yield ()

final case class SkunkReadModelOps[F[_]: Monad: Concurrent: Console](
    pool: Resource[F, Session[F]]
) extends ReadModelOps[F, Event] {
  override def process(event: EventMessage[Event]): F[Unit] = {
    pool.use(s =>
      event.payload match {
        case Created(_, _, "attachment", _, _) => processAttachment(s, event)
        case _                                 => processMetadata(s, event)
      }
    )
  }
}

object SkunkReadModelOps {
  def make[F[_]: Async: Sync: Console](
      pool: Resource[F, Session[F]]
  ): F[SkunkReadModelOps[F]] =
    Sync[F].delay(new SkunkReadModelOps[F](pool))
}
