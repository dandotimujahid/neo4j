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
package org.neo4j.cypher.internal.runtime.ast

import org.neo4j.cypher.internal.ast.semantics.SemanticCheck
import org.neo4j.cypher.internal.ast.semantics.SemanticCheckableExpression
import org.neo4j.cypher.internal.expressions.Expression.SemanticContext
import org.neo4j.cypher.internal.expressions.LogicalVariable
import org.neo4j.cypher.internal.util.InputPosition
import org.neo4j.exceptions.InternalException

abstract class RuntimeVariable(override val name: String) extends LogicalVariable with SemanticCheckableExpression {
  override def semanticCheck(ctx: SemanticContext): SemanticCheck = SemanticCheck.success

  override def position: InputPosition = InputPosition.NONE

  override def isIsolated: Boolean = fail()

  override def copyId = fail()

  override def withPosition(position: InputPosition): LogicalVariable = fail()

  override def renameId(newName: String) = fail()

  private def fail(): Nothing = throw new InternalException("Tried using a RuntimeVariable as Variable")
}
