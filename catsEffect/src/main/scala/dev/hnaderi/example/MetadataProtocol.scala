package dev.hnaderi.example

import dev.hnaderi.example.metadata.MetadataItem
import dev.hnaderi.example.metadata.EntityId
import java.util.UUID

case class MetadataCreation(metadataId:UUID, parent: Option[UUID], entityId: Option[EntityId], category: String,
                            items: List[MetadataItemCreation])
case class MetadataItemCreation(name: String, value: String)
case class MetadataItems(metadataId:UUID, items:List[MetadataItem])