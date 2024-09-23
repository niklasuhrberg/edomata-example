package dev.hnaderi.example.metadata


import cats.data.ValidatedNec
import cats.implicits.*
import edomata.core.*
import edomata.syntax.all.*

import java.util.UUID

case class MetadataItem(id: UUID, name: String, value: String)

enum Event {
  case Created(entityId: String, parent: Option[UUID], category: String, user: String, items: List[MetadataItem])
  case ItemAdded(item: MetadataItem, user:String)
}

enum Rejection {
  case IllegalState
  case InvalidItem
}

enum Metadata {
  case Initialized(entityId: String, parent: Option[UUID], category: String, contents: List[MetadataItem])
  case New


  def create(entityId: String, parent: Option[UUID], category: String, user: String, items: List[MetadataItem]): Decision[Rejection, Event, Metadata] = this.decide {
    case New => Decision.accept(Event.Created(entityId, parent, category, user, items))
    case _ => Decision.reject(Rejection.IllegalState)
  }

  def addItem(item: MetadataItem, user: String): Decision[Rejection, Event, Metadata] = this.decide {
    case New => Decision.reject(Rejection.IllegalState)
    case Initialized(_, _, _,_) => Decision.accept(Event.ItemAdded(item, user))
  }

  private def mustBeNew: ValidatedNec[Rejection, Metadata] = this match {
    case New => New.validNec
    case _ => Rejection.IllegalState.invalidNec
  }

  private def mustBeInitialized: ValidatedNec[Rejection, Initialized] = this match {
    case i@Initialized(_, _, _, _) => i.validNec
    case _ => Rejection.IllegalState.invalidNec
  }
}

object Metadata extends DomainModel[Metadata, Event, Rejection] {
  def initial = New

  def transition = {
    case Event.ItemAdded(item, user) => _.mustBeInitialized.map(i => i.copy(contents = i.contents.appended(item)))
    case Event.Created(entityId, parent,category, user, items) => _.mustBeNew.map(n =>
      Initialized(entityId = entityId,
        parent = parent,
      category = category,
        contents = items))
    }
}

