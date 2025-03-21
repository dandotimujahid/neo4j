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
import org.neo4j.cypher.internal.util.symbols.CTInteger
import org.neo4j.cypher.internal.util.symbols.CTString

case object Substring extends Function {
  def name = "substring"

  override val signatures = Vector(
    FunctionTypeSignature(
      function = this,
      names = Vector("original", "start"),
      argumentTypes = Vector(CTString, CTInteger),
      outputType = CTString,
      description = "Returns a substring of the given `STRING`, beginning with a 0-based index start.",
      category = Category.STRING,
      argumentDescriptions =
        Map("original" -> "The string to be shortened.", "start" -> "The start position of the new string.")
    ),
    FunctionTypeSignature(
      function = this,
      names = Vector("original", "start", "length"),
      argumentTypes = Vector(CTString, CTInteger, CTInteger),
      outputType = CTString,
      description =
        "Returns a substring of a given `length` from the given `STRING`, beginning with a 0-based index start.",
      category = Category.STRING,
      argumentDescriptions = Map(
        "original" -> "The string to be shortened.",
        "start" -> "The start position of the new string.",
        "length" -> "The length of the new string."
      )
    )
  )
}
