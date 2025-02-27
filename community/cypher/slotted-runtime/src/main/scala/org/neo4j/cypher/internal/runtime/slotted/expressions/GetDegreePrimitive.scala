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
package org.neo4j.cypher.internal.runtime.slotted.expressions

import org.neo4j.cypher.internal.expressions.SemanticDirection
import org.neo4j.cypher.internal.runtime.ReadableRow
import org.neo4j.cypher.internal.runtime.interpreted.commands.AstNode
import org.neo4j.cypher.internal.runtime.interpreted.commands.expressions.Expression
import org.neo4j.cypher.internal.runtime.interpreted.pipes.LazyType
import org.neo4j.cypher.internal.runtime.interpreted.pipes.QueryState
import org.neo4j.values.AnyValue
import org.neo4j.values.storable.Values
import org.neo4j.values.storable.Values.longValue

case class GetDegreePrimitive(offset: Int, direction: SemanticDirection)
    extends Expression
    with SlottedExpression {

  override def apply(row: ReadableRow, state: QueryState): AnyValue =
    longValue(state.query.nodeGetDegree(row.getLongAt(offset), direction, state.cursors.nodeCursor))
  override def children: Seq[AstNode[_]] = Seq.empty
}

case class GetDegreeWithTypePrimitive(offset: Int, typeId: Int, direction: SemanticDirection)
    extends Expression
    with SlottedExpression {

  override def apply(row: ReadableRow, state: QueryState): AnyValue =
    longValue(state.query.nodeGetDegree(row.getLongAt(offset), direction, typeId, state.cursors.nodeCursor))
  override def children: Seq[AstNode[_]] = Seq.empty
}

case class GetDegreeWithTypePrimitiveLate(offset: Int, typ: LazyType, direction: SemanticDirection)
    extends Expression
    with SlottedExpression {

  override def apply(row: ReadableRow, state: QueryState): AnyValue = {
    val typeId = typ.getId(row, state)
    if (typeId == LazyType.UNKNOWN) Values.ZERO_INT
    else longValue(state.query.nodeGetDegree(row.getLongAt(offset), direction, typeId, state.cursors.nodeCursor))
  }

  override def children: Seq[AstNode[_]] = Seq.empty
}
