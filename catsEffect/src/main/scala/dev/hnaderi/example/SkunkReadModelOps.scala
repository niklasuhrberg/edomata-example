package dev.hnaderi.example
import cats.{Applicative, Monad}
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
import skunk.data.Completion.Insert

import java.time.{Instant, OffsetDateTime, ZoneId}
import java.util.UUID

case class InsertMessageToEntityRow(
    messageId: UUID,
    entityId: UUID
                                   )
case class InsertEntityRow(
    id: UUID,
    entityType: String,
    entityId: String
                          )

case class InsertMessageRow(
    id: UUID,
    predecessor: Option[UUID],
    sequenceNumber: Int,
    subject: Option[String],
    content: String,
    audience: String,
    createdBy: String,
    systemOrigin: String,
    createdAt: Instant
)

case class InsertAuditRow(
    id: UUID,
    metadataId: UUID,
    action: String,
    username: String,
    time: Instant
)
case class InsertMetadataRow(
    id: UUID,
    entityId: String,
    entityType: String,
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
    value: String
)




final case class SkunkReadModelOps[F[_]: Monad: Concurrent: Console](
    pool: Resource[F, Session[F]]
) extends ReadModelOps[F, Event] {
  override def process(event: EventMessage[Event]): F[Unit] = {
    import SkunkReadModelOps.*
    pool.use(s =>
      event.payload match {
        case Created(_, _, "attachment", _, _) => processAttachment(s, event)
        case Created(_, _, "message", _, _) => processMessage(s, event)
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

  val entityCodec: Codec[InsertEntityRow] =
    (uuid, varchar, varchar).tupled.imap {
      case (id, entityType, entityId) => InsertEntityRow(id, entityType, entityId)
    }  {
      case e => (e.id, e.entityType, e.entityId)
    }
  val messageToEntityCodec: Codec[InsertMessageToEntityRow] =
    (uuid, uuid).tupled.imap {
      case (messageId, entityId) => InsertMessageToEntityRow(messageId = messageId, entityId = entityId)
    }  {
      m => (m.messageId, m.entityId)
    }

  val messageCodec: Codec[InsertMessageRow] =
    (
      uuid,
      uuid.opt,
      int4,
      varchar.opt,
      varchar,
      varchar,
      varchar,
      varchar,
      timestamptz
    ).tupled.imap {
      case (
        id,
        predecessor,
        sequenceNumber,
        subject,
        content,
        audience,
        createdBy,
        systemOrigin,
        createdAt
        ) =>
        InsertMessageRow(
          id = id,
          predecessor = predecessor,
          sequenceNumber = sequenceNumber,
          subject = subject,
          content = content,
          audience = audience,
          createdBy = createdBy,
          systemOrigin = systemOrigin,
          createdAt = createdAt.toInstant()
        )
    } { m =>
      (
        m.id,
        m.predecessor,
        m.sequenceNumber,
        m.subject,
        m.content,
        m.audience,
        m.createdBy,
        m.systemOrigin,
        OffsetDateTime.ofInstant(m.createdAt, ZoneId.systemDefault())
      )
    }

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
    (uuid, varchar, varchar, uuid.opt, varchar, varchar).tupled.imap {
      case (id, entityId, entityType, parent, createdBy, category) =>
        InsertMetadataRow(id, entityId, entityType, parent, createdBy, category)
    } { inr => (inr.id, inr.entityId, inr.entityType, inr.parent, inr.createdBy, inr.category) }
  val itemCodec: Codec[InsertItemRow] =
    (uuid, uuid, varchar, varchar).tupled.imap {
      case (id, metadataId, name, value) =>
        InsertItemRow(id, metadataId, name, value)
    } { item => (item.id, item.metadataId, item.name, item.value) }

  val auditCodec: Codec[InsertAuditRow] =
    (uuid, uuid, varchar, varchar, timestamptz).tupled.imap {
      case (id, metadataId, action, username, time) =>
        InsertAuditRow(id, metadataId, action, username, time.toInstant)
    } { a =>
      (
        a.id,
        a.metadataId,
        a.action,
        a.username,
        OffsetDateTime.ofInstant(a.time, ZoneId.systemDefault())
      )
    }


  def insertEntityCommand: Command[InsertEntityRow] =
    sql"""
         INSERT INTO entities (id, entitytype_id, entity_id) VALUES ($entityCodec) ON CONFLICT DO NOTHING
       """.command
  def insertMessagesToEntitiesCommand: Command[InsertMessageToEntityRow] =
    sql"""
         INSERT INTO messages_entities (message_id, entity_id) VALUES ($messageToEntityCodec)
       """.command
/*
 insert into messages_entities (message_id, entity_id)
 select '0aa412ac-d220-461b-97e3-239d8512ac81', id from entities where entity_id= 'e03ab607-ccd9-472b-a39b-1155c0c89a34' AND entitytype_id='ISOM_ORDER'
 */

  def insertMessagesToEntities2Command: Command[Tuple3[UUID, String, String]] =
    sql"""
           INSERT INTO messages_entities (message_id, entity_id) SELECT $uuid , id from entities where entity_id = $varchar AND entitytype_id=$varchar
         """.command

  def insertMessageCommand: Command[InsertMessageRow] =
    sql"""
           INSERT INTO messages (id, predecessor, seq_nr, subject, content, audience, username, sys_id_origin, created_at)
           VALUES($messageCodec)
         """.command

  def insertCommand: Command[InsertMetadataRow] =
    sql"""
      INSERT INTO metadata VALUES($codec)
      """.command

  def insertItemCommand: Command[InsertItemRow] =
    sql"""
         INSERT INTO items (id, metadata_id, name, value) VALUES($itemCodec)
       """.command

  def insertAttachmentCommand: Command[InsertAttachmentRow] =
    sql"""
      INSERT INTO attachments VALUES($attachemtCodec)
      """.command

  def insertAuditCommand: Command[InsertAuditRow] =
    sql"""
       INSERT INTO audit (id, metadata_id, action, username, time) VALUES ($auditCodec)
     """.command

  def eventToMessageInsert(msg: EventMessage[Event]): InsertMessageRow = {
    val m = msg.payload.asInstanceOf[Event.Created]
    InsertMessageRow(
      id = UUID.fromString(msg.metadata.stream),
      predecessor = m.parent,
      sequenceNumber = 0,
      subject = m.items.find(_.name == "subject").map(_.value),
      content = filterItem(m.items, "content"),
      audience = filterItem(m.items, "audience"),
      createdBy = m.user,
      systemOrigin = filterItem(m.items, "systemOrigin"),
      createdAt = msg.metadata.time.toInstant()
    )
  }

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
      createdAt = eventMessage.metadata.time.toInstant
    )
  }

  def itemsToMetadata(eventMessage: EventMessage[Event]): InsertMetadataRow = {
    val c = eventMessage.payload.asInstanceOf[Event.Created]
    c.entityId.fold(throw IllegalStateException("No entity id"))(entityId =>
      InsertMetadataRow(
        UUID.fromString(eventMessage.metadata.stream),
        entityId.id,
        entityId.entityType,
        c.parent,
        c.user,
        c.category
      )
    )

  }

  def itemToRow(metadataId: UUID, metadataItem: MetadataItem): InsertItemRow =
    InsertItemRow(
      metadataItem.id,
      metadataId,
      metadataItem.name,
      metadataItem.value
    )
  def insertMessageMappingForNewEntity[F[_]: Monad](s: Session[F], entityId: UUID,
                                                    messageId: UUID) = for {
    command <- s.prepare(insertMessagesToEntitiesCommand)
    rowCount <- command.execute(InsertMessageToEntityRow(messageId = messageId, entityId = entityId))
  } yield ()
  def insertMessageMappingForExistingEntity[F[_]: Monad](
                                                          s: Session[F], insertEntity:InsertEntityRow, messageId: UUID) = for {
    command <- s.prepare(insertMessagesToEntities2Command)
    rowCount <- command.execute(Tuple3(messageId, insertEntity.entityId, insertEntity.entityType))
  } yield()

  def processMessage[F[_] : Monad](
                                    s: Session[F],
                                    event: EventMessage[Event]
                                  ) = for {
    createdEvent <- event.payload.asInstanceOf[Event.Created].pure
    insertMessage <- eventToMessageInsert(event).pure
    command <- s.prepare(insertMessageCommand)
    rowcount <- command.execute(eventToMessageInsert(event))
    // Insert entity, use id if rowcount == 1
    insertEntity <- InsertEntityRow(UUID.randomUUID(),
      createdEvent.entityId.get.entityType,
      createdEvent.entityId.get.id).pure
    entityCommand <- s.prepare(insertEntityCommand)
    entityRowCount <- entityCommand.execute(insertEntity)
    _ <- entityRowCount match {
      case Insert(0) => insertMessageMappingForExistingEntity(s, insertEntity, UUID.fromString(event.metadata.stream))
      case Insert(1) => insertMessageMappingForNewEntity(s, insertEntity.id, UUID.fromString(event.metadata.stream))
    }
    // Insert mapping , if entity rowcount == 0, use embedded select

  } yield ()

  def processAttachment[F[_] : Monad : Console](
                                                 s: Session[F],
                                                 event: EventMessage[Event]
                                               ): F[Unit] = for {
    insertAttachment <- itemsToAttachment(event).pure
    command <- s.prepare(insertAttachmentCommand)
    rowCount <- command.execute(itemsToAttachment(event))
    auditCommand <- s.prepare(insertAuditCommand)
    rowCountAudit <- auditCommand.execute(
      InsertAuditRow(
        UUID.randomUUID(),
        insertAttachment.id,
        "Create attachment",
        "default user",
        Instant.now()
      )
    )
  } yield ()

  def processItem[F[_] : Monad : Console](
                                           s: Session[F],
                                           item: MetadataItem,
                                           metadataId: UUID
                                         ): F[Completion] = for {
    itemCommand <- s.prepare(insertItemCommand)
    r <- itemCommand.execute(itemToRow(metadataId, item))
  } yield r

  def processMetadata[F[_] : Monad : Console](
                                               s: Session[F],
                                               event: EventMessage[Event]
                                             ): F[Unit] = for {
    m <- itemsToMetadata(event).pure
    command <- s.prepare(insertCommand)
    rowCount <- command.execute(m)
    r <- event.payload
      .asInstanceOf[Created]
      .items
      .traverse(i => processItem[F](s, i, UUID.fromString(event.metadata.stream)))
    auditCommand <- s.prepare(insertAuditCommand)
    a <- auditCommand.execute(
      InsertAuditRow(
        UUID.randomUUID(),
        m.id,
        "Created metadata",
        "default user",
        Instant.now()
      )
    )
    //  itemCommand <- s.prepare(insertItemCommand)
    //  r <- event.payload.asInstanceOf[Created].items.map(i =>
    //    itemCommand.execute(itemToRow(UUID.fromString(event.metadata.stream), i))).reduce((l,r) => l)
  } yield ()

}
