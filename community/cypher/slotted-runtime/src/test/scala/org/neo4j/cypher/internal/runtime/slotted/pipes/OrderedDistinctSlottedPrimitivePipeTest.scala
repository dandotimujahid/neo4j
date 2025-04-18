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
package org.neo4j.cypher.internal.runtime.slotted.pipes

import org.neo4j.cypher.internal.physicalplanning.SlotConfigurationBuilder
import org.neo4j.cypher.internal.runtime.ResourceManager
import org.neo4j.cypher.internal.runtime.interpreted.QueryStateHelper
import org.neo4j.cypher.internal.util.symbols.CTNode
import org.neo4j.cypher.internal.util.test_helpers.CypherFunSuite
import org.neo4j.kernel.impl.util.collection.DistinctSet

class OrderedDistinctSlottedPrimitivePipeTest extends CypherFunSuite {

  test("exhaust should close seen set") {
    val monitor = QueryStateHelper.trackClosedMonitor
    val resourceManager = new ResourceManager(monitor)
    val slots = SlotConfigurationBuilder.empty
      .newLong("a", nullable = false, CTNode)
      .newLong("b", nullable = false, CTNode)
      .build()

    val input = FakeSlottedPipe(Seq(Map("a" -> 10, "b" -> 11)), slots)
    val pipe = OrderedDistinctSlottedPrimitivePipe(input, slots, Array(0), Array(1), EmptyGroupingExpression)()
    // exhaust
    pipe.createResults(QueryStateHelper.emptyWithResourceManager(resourceManager)).toList
    input.wasClosed shouldBe true
    monitor.closedResources.collect { case t: DistinctSet[_] => t } should have size (1)
  }

  test("close should close seen set") {
    val monitor = QueryStateHelper.trackClosedMonitor
    val resourceManager = new ResourceManager(monitor)
    val slots = SlotConfigurationBuilder.empty
      .newLong("a", nullable = false, CTNode)
      .newLong("b", nullable = false, CTNode)
      .build()

    val input = FakeSlottedPipe(Seq(Map("a" -> 10)), slots)
    val pipe = OrderedDistinctSlottedPrimitivePipe(input, slots, Array(0), Array(1), EmptyGroupingExpression)()
    val result = pipe.createResults(QueryStateHelper.emptyWithResourceManager(resourceManager))
    result.close()
    input.wasClosed shouldBe true
    monitor.closedResources.collect { case t: DistinctSet[_] => t } should have size (1)
  }
}
