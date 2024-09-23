package dev.hnaderi.example.metadata

import dev.hnaderi.example.metadata.Command.{AddItem, Create}
import dev.hnaderi.example.metadata.Notification.{ItemAdded, MetadataCreated}

import java.util.UUID

enum Command {
  case Create(entityId: String, parent: Option[UUID], category: String, user: String, items: List[MetadataItem])
  case AddItem(item: MetadataItem, user: String)
}

enum Notification {
  case MetadataCreated(metadataId: UUID, entityId: String)
  case ItemAdded(metadataId: UUID, item: MetadataItem)
}

object MetadataService extends Metadata.Service[Command, Notification] {

  import cats.Monad

  def apply[F[_] : Monad]: App[F, Unit] = App.router {
    case Create(entityId, parent, category, user, items) => 
      for {
        ns <- App.state.decide(_.create(entityId, parent, category, user, items))
        aggregateId <- App.aggregateId
        _ <- App.publish(MetadataCreated(UUID.fromString(aggregateId), entityId))
      } yield()
    case AddItem(item, user) => 
      for {
        ns <- App.state.decide(_.addItem(item, user))
        aggregateId <- App.aggregateId
        _ <- App.publish(ItemAdded(UUID.fromString(aggregateId), item))
      } yield ()
  }
}
