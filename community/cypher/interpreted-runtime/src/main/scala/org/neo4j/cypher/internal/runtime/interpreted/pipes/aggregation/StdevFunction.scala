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
package org.neo4j.cypher.internal.runtime.interpreted.pipes.aggregation

import org.neo4j.cypher.internal.runtime.ReadableRow
import org.neo4j.cypher.internal.runtime.interpreted.commands.expressions.Expression
import org.neo4j.cypher.internal.runtime.interpreted.pipes.QueryState
import org.neo4j.memory.HeapEstimator.shallowSizeOfInstance
import org.neo4j.values.AnyValue
import org.neo4j.values.storable.Values

/**
 * Taking inspiration from:
 *
 * http://www.alias-i.com/lingpipe/src/com/aliasi/stats/OnlineNormalEstimator.java
 * https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Welford's_online_algorithm
 */
class StdevFunction(val value: Expression, val population: Boolean) extends NumericExpressionOnly {

  def name: String = if (population) "STDEVP" else "STDEV"

  var count: Int = 0
  var movingAvg: Double = 0.0
  // sum of squares of differences from the current mean
  var m2: Double = 0.0

  override def result(state: QueryState): AnyValue = {
    if (count < 2) {
      Values.ZERO_FLOAT
    } else {
      val variance = if (population) m2 / count else m2 / (count - 1)
      Values.doubleValue(math.sqrt(variance))
    }
  }

  override def apply(data: ReadableRow, state: QueryState): Unit = {
    actOnNumber(
      value(data, state),
      number => {
        count += 1
        val x = number.doubleValue()
        val nextM = movingAvg + (x - movingAvg) / count
        m2 += (x - movingAvg) * (x - nextM)
        movingAvg = nextM
      },
      state
    )
  }
}

object StdevFunction {
  val SHALLOW_SIZE: Long = shallowSizeOfInstance(classOf[StdevFunction])
}
