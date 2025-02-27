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
package org.neo4j.cypher.internal.compiler.planner.logical

import org.neo4j.cypher.internal.compiler.ExecutionModel
import org.neo4j.cypher.internal.compiler.planner.logical.Metrics.CostModel
import org.neo4j.cypher.internal.compiler.planner.logical.Metrics.QueryGraphCardinalityModel
import org.neo4j.cypher.internal.compiler.planner.logical.Metrics.SelectivityCalculator
import org.neo4j.cypher.internal.compiler.planner.logical.cardinality.QueryGraphCardinalityModel
import org.neo4j.cypher.internal.compiler.planner.logical.cardinality.assumeIndependence.LabelInferenceStrategy
import org.neo4j.cypher.internal.planner.spi.PlanContext
import org.neo4j.cypher.internal.util.CancellationChecker

object SimpleMetricsFactory extends MetricsFactory {

  override def newCostModel(executionModel: ExecutionModel, cancellationChecker: CancellationChecker): CostModel =
    CardinalityCostModel(executionModel, cancellationChecker)

  override def newCardinalityEstimator(
    queryGraphCardinalityModel: QueryGraphCardinalityModel,
    selectivityCalculator: SelectivityCalculator,
    expressionEvaluator: ExpressionEvaluator
  ): StatisticsBackedCardinalityModel =
    new StatisticsBackedCardinalityModel(queryGraphCardinalityModel, selectivityCalculator, expressionEvaluator)

  override def newQueryGraphCardinalityModel(
    planContext: PlanContext,
    selectivityCalculator: SelectivityCalculator,
    labelInferenceStrategy: LabelInferenceStrategy = LabelInferenceStrategy.NoInference
  ): QueryGraphCardinalityModel =
    QueryGraphCardinalityModel.default(planContext, selectivityCalculator, labelInferenceStrategy)
}
