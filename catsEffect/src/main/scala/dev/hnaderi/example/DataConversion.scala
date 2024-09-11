package dev.hnaderi.example

import dev.hnaderi.example.metadata.MetadataItem

object DataConversion {

  def itemCreationToItem(mc: MetadataItemCreation) = MetadataItem(id = mc.id,
    name = mc.name,
    value = mc.value,
    category = mc.category, parent = mc.parent, createdBy = mc.createdBy)

}
