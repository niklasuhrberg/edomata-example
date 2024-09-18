package dev.hnaderi.example

import dev.hnaderi.example.metadata.MetadataItem

import java.util.UUID

object DataConversion {

  def itemCreationToItem(mc: MetadataItemCreation) = MetadataItem(id = UUID.randomUUID(),
    name = mc.name,
    value = mc.value)

}
