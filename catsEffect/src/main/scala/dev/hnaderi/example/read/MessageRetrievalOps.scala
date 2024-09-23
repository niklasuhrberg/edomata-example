package dev.hnaderi.example.read
import fs2.Stream
import cats.Monad
import cats.effect.{Async, Sync,  Concurrent, Resource}
import cats.effect.std.Console
import dev.hnaderi.example.{InsertMessageRow, SkunkReadModelOps}
import skunk.{Query, Session}
import skunk.implicits.sql
import skunk.codec.all.*
import skunk.syntax.all.*
import cats.syntax.all.*
import java.time.Instant
import java.util.UUID

case class Message(
    id: UUID,
    responseTo: Option[UUID],
    sequenceNumber: Int,
    subject: Option[String],
    content: String,
    audience: String,
    createdBy: String,
    systemOrigin: String,
    createdAt: Instant
)

case class MessageThread(start: Instant, messages: List[Message])

val selectMessagesByEntityId: Query[UUID, InsertMessageRow] =
  sql"""
     SELECT m.id,
          m.predecessor,
          m.seq_nr,
          m.subject,
          m.content,
          m.audience,
          m.username,
          m.sys_id_origin,
          m.created_at FROM
          messages m join messages_entities me on me.message_id = m.id WHERE me.entity_id = $uuid
       """.query(SkunkReadModelOps.messageCodec)
case class MessageRetrievalOps[F[_]: Async: Sync: Console](
    pool: Resource[F, Session[F]]
) {
  def messagesForEntity(entityId: UUID): F[Stream[F, InsertMessageRow]] = {
    pool.use(session =>
      session.prepare[UUID, InsertMessageRow](selectMessagesByEntityId)
       .map(pq => pq.stream(entityId, 32)))
  }
}

object MessageRetrievalOps {
  def make[F[_]: Async: Sync: Console](
      pool: Resource[F, Session[F]]
  ): F[MessageRetrievalOps[F]] =
    Sync[F].delay(new MessageRetrievalOps[F](pool))
}
