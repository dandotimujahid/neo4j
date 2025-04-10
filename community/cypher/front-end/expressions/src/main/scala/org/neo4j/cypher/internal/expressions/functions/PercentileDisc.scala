/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
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
package org.neo4j.cypher.internal.expressions.functions

import org.neo4j.cypher.internal.expressions.FunctionTypeSignature
import org.neo4j.cypher.internal.util.InputPosition
import org.neo4j.cypher.internal.util.symbols.CTFloat
import org.neo4j.cypher.internal.util.symbols.CTInteger
import org.neo4j.cypher.internal.util.symbols.ClosedDynamicUnionType

case object PercentileDisc extends AggregatingFunction {
  def name = "percentileDisc"

  override val signatures = Vector(
    FunctionTypeSignature(
      function = this,
      names = Vector("input", "percentile"),
      argumentTypes = Vector(ClosedDynamicUnionType(Set(CTInteger, CTFloat))(InputPosition.NONE), CTFloat),
      outputType = ClosedDynamicUnionType(Set(CTInteger, CTFloat))(InputPosition.NONE),
      description =
        "Returns the nearest `INTEGER` or `FLOAT` value to the given percentile over a group using a rounding method.",
      category = Category.AGGREGATING,
      argumentDescriptions =
        Map("input" -> "A value to be aggregated.", "percentile" -> "A percentile between 0.0 and 1.0.")
    )
  )
}
