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
package org.neo4j.cypher.internal.expressions

import org.neo4j.cypher.internal.label_expressions.LabelExpressionDynamicLeafExpression
import org.neo4j.cypher.internal.label_expressions.LabelExpressionLeafName
import org.neo4j.cypher.internal.util.ASTNode
import org.neo4j.cypher.internal.util.InputPosition

trait SymbolicName extends ASTNode {
  def name: String
  def position: InputPosition
  override def asCanonicalStringVal: String = name
}

sealed trait ElementTypeName

sealed trait RelTypeExpression extends ASTNode

case class Namespace(parts: List[String] = List.empty)(val position: InputPosition) extends ASTNode

case class ProcedureName(name: String)(val position: InputPosition) extends ASTNode with SymbolicName

case class ProcedureOutput(name: String)(val position: InputPosition) extends ASTNode with SymbolicName

case class LabelName(name: String)(val position: InputPosition) extends LabelExpressionLeafName with ElementTypeName

case class PropertyKeyName(name: String)(val position: InputPosition) extends SymbolicName

case class RelTypeName(name: String)(val position: InputPosition) extends LabelExpressionLeafName
    with ElementTypeName
    with RelTypeExpression {
  override def asCanonicalStringVal: String = name
}

case class LabelOrRelTypeName(name: String)(val position: InputPosition) extends LabelExpressionLeafName {
  def asLabelName: LabelName = LabelName(name)(position)
  def asRelTypeName: RelTypeName = RelTypeName(name)(position)
}

case class DynamicLabelExpression(expression: Expression, all: Boolean = true)(val position: InputPosition)
    extends LabelExpressionDynamicLeafExpression with ElementTypeName {

  override def mapExpressions(f: Expression => Expression): LabelExpressionDynamicLeafExpression = copy(
    expression = f(expression)
  )(this.position)
}

case class DynamicRelTypeExpression(expression: Expression, all: Boolean = true)(val position: InputPosition)
    extends LabelExpressionDynamicLeafExpression
    with ElementTypeName
    with RelTypeExpression {
  override def asCanonicalStringVal: String = s"$$${if (all) "all" else "any"}(${expression.asCanonicalStringVal})"

  override def mapExpressions(f: Expression => Expression): LabelExpressionDynamicLeafExpression = copy(
    expression = f(expression)
  )(this.position)
}

case class DynamicLabelOrRelTypeExpression(expression: Expression, all: Boolean = true)(val position: InputPosition)
    extends LabelExpressionDynamicLeafExpression {
  def asDynamicLabelExpression: DynamicLabelExpression = DynamicLabelExpression(expression, all)(position)
  def asDynamicRelTypeExpression: DynamicRelTypeExpression = DynamicRelTypeExpression(expression, all)(position)

  override def mapExpressions(f: Expression => Expression): LabelExpressionDynamicLeafExpression = copy(
    expression = f(expression)
  )(this.position)
}
