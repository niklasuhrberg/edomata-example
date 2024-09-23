package dev.hnaderi.example

import dev.hnaderi.example.metadata.MetadataItem

import java.util.UUID

case class MetadataCreation(metadataId:UUID, parent: Option[UUID], entityId:String, category: String,
                            items: List[MetadataItemCreation])
case class MetadataItemCreation(name: String, value: String)
case class MetadataItems(metadataId:UUID, items:List[MetadataItem])