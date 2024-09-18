/*
 * Copyright 2023 Hossein Naderi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.hnaderi.example
package accounts

import cats.Id
import dev.hnaderi.example.metadata.{Command, Metadata, MetadataItem, MetadataService, Notification}
import edomata.munit.DomainSuite

import java.util.UUID


class DomainLogicSuite extends DomainSuite(msgId = "msg", address = "20187a0d-703d-4f52-9915-3cb7fad57e8e") {
  test("Test") {
    val entityId = UUID.randomUUID()
    val item = MetadataItem(UUID.randomUUID(), "filename", "Kitchen Measurements")
    MetadataService[Id].expect(Command.Create(entityId, None, "categoryName", "niuhr2", List(item)), Metadata.New)(
      Metadata.Initialized(entityId, None, "categoryName", List(item)),
      Notification.MetadataCreated(metadataId = UUID.fromString("20187a0d-703d-4f52-9915-3cb7fad57e8e"),
        entityId = entityId)
    )
  }
}
