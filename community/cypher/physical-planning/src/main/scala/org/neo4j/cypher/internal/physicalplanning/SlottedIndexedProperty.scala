/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.physicalplanning

import org.neo4j.cypher.internal.expressions.LogicalVariable
import org.neo4j.cypher.internal.logical.plans.IndexedProperty

object SlottedIndexedProperty {

  def apply(node: LogicalVariable, property: IndexedProperty, slots: SlotConfiguration): SlottedIndexedProperty = {
    val maybeOffset =
      if (property.shouldGetValue) {
        Some(slots.cachedPropOffset(property.asCachedProperty(node).runtimeKey))
      } else {
        None
      }
    SlottedIndexedProperty(property.propertyKeyToken.nameId.id, maybeOffset)
  }
}

case class SlottedIndexedProperty(propertyKeyId: Int, maybeCachedEntityPropertySlot: Option[Int]) {
  def getValueFromIndex: Boolean = maybeCachedEntityPropertySlot.isDefined
}
