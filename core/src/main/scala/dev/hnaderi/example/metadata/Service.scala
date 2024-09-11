package dev.hnaderi.example.metadata

import dev.hnaderi.example.accounts.AccountService.App
import dev.hnaderi.example.metadata.Command.{AddItem, Create}
import dev.hnaderi.example.metadata.Notification.{ItemAdded, MetadataCreated}

import java.util.UUID

enum Command {
  case Create(entityId: UUID, category: String)
  case AddItem(item: MetadataItem)
}

enum Notification {
  case MetadataCreated(metadataId: UUID, entityId: UUID)
  case ItemAdded(metadataId: UUID, item: MetadataItem)
}

object MetadataService extends Metadata.Service[Command, Notification] {

  import cats.Monad

  def apply[F[_] : Monad]: App[F, Unit] = App.router {
    case Create(entityId, category) => 
      for {
        ns <- App.state.decide(_.create(entityId, category))
        aggregateId <- App.aggregateId
        _ <- App.publish(MetadataCreated(UUID.fromString(aggregateId), entityId))
      } yield()
    case AddItem(item) => 
      for {
        ns <- App.state.decide(_.addItem(item))
        aggregateId <- App.aggregateId
        _ <- App.publish(ItemAdded(UUID.fromString(aggregateId), item))
      } yield ()
  }
}
