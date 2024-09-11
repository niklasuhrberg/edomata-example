package dev.hnaderi.example.metadata


import cats.data.ValidatedNec
import cats.implicits.*
import edomata.core.*
import edomata.syntax.all.*

import java.util.UUID

case class MetadataItem(id: UUID, name: String, value: String, category: String, parent: Option[UUID], createdBy: String)

enum Event {
  case Created(metadataId: UUID, category: String)
  case ItemAdded(item: MetadataItem)
}

enum Rejection {
  case IllegalState
  case InvalidItem
}

enum Metadata {
  case Initialized(metadataId: UUID, category: String, contents: List[MetadataItem])
  case New


  def create(metadataId: UUID, category: String): Decision[Rejection, Event, Metadata] = this.decide {
    case New => Decision.accept(Event.Created(metadataId, category))
    case _ => Decision.reject(Rejection.IllegalState)
  }

  def addItem(item: MetadataItem): Decision[Rejection, Event, Metadata] = this.decide {
    case New => Decision.reject(Rejection.IllegalState)
    case Initialized(_, _, _) => Decision.accept(Event.ItemAdded(item))
  }

  private def mustBeNew: ValidatedNec[Rejection, Metadata] = this match {
    case New => New.validNec
    case _ => Rejection.IllegalState.invalidNec
  }

  private def mustBeInitialized: ValidatedNec[Rejection, Initialized] = this match {
    case i@Initialized(_, _, _) => i.validNec
    case _ => Rejection.IllegalState.invalidNec
  }
}

object Metadata extends DomainModel[Metadata, Event, Rejection] {
  def initial = New

  def transition = {
    case Event.ItemAdded(item) => _.mustBeInitialized.map(i => i.copy(contents = i.contents.appended(item)))
    case Event.Created(metadataId, category) => _.mustBeNew.map(n => Initialized(metadataId = metadataId,
      category = category, contents = List.empty))
    }
}

