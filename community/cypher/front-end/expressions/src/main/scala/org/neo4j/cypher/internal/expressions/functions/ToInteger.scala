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

import org.neo4j.cypher.internal.expressions.TypeSignature
import org.neo4j.cypher.internal.util.InputPosition
import org.neo4j.cypher.internal.util.symbols.CTBoolean
import org.neo4j.cypher.internal.util.symbols.CTFloat
import org.neo4j.cypher.internal.util.symbols.CTInteger
import org.neo4j.cypher.internal.util.symbols.CTString
import org.neo4j.cypher.internal.util.symbols.ClosedDynamicUnionType

case object ToInteger extends Function {
  override def name = "toInteger"

  override val signatures = Vector(
    TypeSignature(
      this,
      ClosedDynamicUnionType(Set(CTString, CTInteger, CTFloat, CTBoolean))(InputPosition.NONE),
      CTInteger,
      "Converts a `BOOLEAN`, `STRING`, `INTEGER` or `FLOAT` value to an `INTEGER` value. For `BOOLEAN` values, true is defined to be 1 and false is defined to be 0.",
      Category.SCALAR
    )
  )
}
