package dev.hnaderi.example

import dev.hnaderi.example.metadata.MetadataItem

import java.util.UUID

case class MetadataCreation(metadataId:UUID, entityId:UUID, category: String)
case class MetadataItemCreation(id: UUID, name: String, value: String, category: String, parent: Option[UUID], createdBy: String)
case class MetadataItems(metadataId:UUID, items:List[MetadataItem])