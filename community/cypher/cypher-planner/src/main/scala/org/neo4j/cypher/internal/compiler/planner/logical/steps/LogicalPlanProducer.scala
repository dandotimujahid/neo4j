/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.compiler.planner.logical.steps

import org.neo4j.cypher.internal.ast.CommandClause
import org.neo4j.cypher.internal.ast.Hint
import org.neo4j.cypher.internal.ast.ShowConstraintsClause
import org.neo4j.cypher.internal.ast.ShowFunctionsClause
import org.neo4j.cypher.internal.ast.ShowIndexesClause
import org.neo4j.cypher.internal.ast.ShowProceduresClause
import org.neo4j.cypher.internal.ast.ShowSettingsClause
import org.neo4j.cypher.internal.ast.ShowTransactionsClause
import org.neo4j.cypher.internal.ast.SubqueryCall.InTransactionsErrorParameters
import org.neo4j.cypher.internal.ast.SubqueryCall.InTransactionsOnErrorBehaviour
import org.neo4j.cypher.internal.ast.SubqueryCall.InTransactionsParameters
import org.neo4j.cypher.internal.ast.SubqueryCall.InTransactionsReportParameters
import org.neo4j.cypher.internal.ast.TerminateTransactionsClause
import org.neo4j.cypher.internal.ast.Union.UnionMapping
import org.neo4j.cypher.internal.ast.UsingIndexHint
import org.neo4j.cypher.internal.ast.UsingJoinHint
import org.neo4j.cypher.internal.ast.UsingScanHint
import org.neo4j.cypher.internal.compiler.ExecutionModel
import org.neo4j.cypher.internal.compiler.helpers.PredicateHelper.coercePredicatesWithAnds
import org.neo4j.cypher.internal.compiler.planner.ProcedureCallProjection
import org.neo4j.cypher.internal.compiler.planner.logical.LogicalPlanningContext
import org.neo4j.cypher.internal.compiler.planner.logical.Metrics.CardinalityModel
import org.neo4j.cypher.internal.compiler.planner.logical.ordering.InterestingOrderConfig
import org.neo4j.cypher.internal.compiler.planner.logical.steps.index.ContainsSearchMode
import org.neo4j.cypher.internal.compiler.planner.logical.steps.index.EndsWithSearchMode
import org.neo4j.cypher.internal.compiler.planner.logical.steps.index.StringSearchMode
import org.neo4j.cypher.internal.compiler.planner.logical.steps.skipAndLimit.planLimitOnTopOf
import org.neo4j.cypher.internal.expressions.Add
import org.neo4j.cypher.internal.expressions.CachedHasProperty
import org.neo4j.cypher.internal.expressions.CachedProperty
import org.neo4j.cypher.internal.expressions.Equals
import org.neo4j.cypher.internal.expressions.Expression
import org.neo4j.cypher.internal.expressions.FilterScope
import org.neo4j.cypher.internal.expressions.FunctionInvocation
import org.neo4j.cypher.internal.expressions.LabelName
import org.neo4j.cypher.internal.expressions.LabelToken
import org.neo4j.cypher.internal.expressions.MapProjection
import org.neo4j.cypher.internal.expressions.PatternComprehension
import org.neo4j.cypher.internal.expressions.PatternExpression
import org.neo4j.cypher.internal.expressions.Property
import org.neo4j.cypher.internal.expressions.PropertyKeyName
import org.neo4j.cypher.internal.expressions.RelTypeName
import org.neo4j.cypher.internal.expressions.RelationshipTypeToken
import org.neo4j.cypher.internal.expressions.SemanticDirection
import org.neo4j.cypher.internal.expressions.SemanticDirection.BOTH
import org.neo4j.cypher.internal.expressions.SignedDecimalIntegerLiteral
import org.neo4j.cypher.internal.expressions.StringLiteral
import org.neo4j.cypher.internal.expressions.Variable
import org.neo4j.cypher.internal.expressions.functions.Collect
import org.neo4j.cypher.internal.expressions.functions.UnresolvedFunction
import org.neo4j.cypher.internal.ir.AggregatingQueryProjection
import org.neo4j.cypher.internal.ir.CSVFormat
import org.neo4j.cypher.internal.ir.CallSubqueryHorizon
import org.neo4j.cypher.internal.ir.CommandProjection
import org.neo4j.cypher.internal.ir.CreateNode
import org.neo4j.cypher.internal.ir.CreatePattern
import org.neo4j.cypher.internal.ir.CreateRelationship
import org.neo4j.cypher.internal.ir.DeleteExpression
import org.neo4j.cypher.internal.ir.DistinctQueryProjection
import org.neo4j.cypher.internal.ir.EagernessReason
import org.neo4j.cypher.internal.ir.ForeachPattern
import org.neo4j.cypher.internal.ir.LoadCSVProjection
import org.neo4j.cypher.internal.ir.MergeNodePattern
import org.neo4j.cypher.internal.ir.MergeRelationshipPattern
import org.neo4j.cypher.internal.ir.NodeBinding
import org.neo4j.cypher.internal.ir.NodeConnection
import org.neo4j.cypher.internal.ir.PassthroughAllHorizon
import org.neo4j.cypher.internal.ir.PatternRelationship
import org.neo4j.cypher.internal.ir.PlannerQuery
import org.neo4j.cypher.internal.ir.QuantifiedPathPattern
import org.neo4j.cypher.internal.ir.QueryGraph
import org.neo4j.cypher.internal.ir.QueryProjection
import org.neo4j.cypher.internal.ir.RegularSinglePlannerQuery
import org.neo4j.cypher.internal.ir.RemoveLabelPattern
import org.neo4j.cypher.internal.ir.Selections
import org.neo4j.cypher.internal.ir.SetLabelPattern
import org.neo4j.cypher.internal.ir.SetMutatingPattern
import org.neo4j.cypher.internal.ir.SetNodePropertiesFromMapPattern
import org.neo4j.cypher.internal.ir.SetNodePropertiesPattern
import org.neo4j.cypher.internal.ir.SetNodePropertyPattern
import org.neo4j.cypher.internal.ir.SetPropertiesFromMapPattern
import org.neo4j.cypher.internal.ir.SetPropertiesPattern
import org.neo4j.cypher.internal.ir.SetPropertyPattern
import org.neo4j.cypher.internal.ir.SetRelationshipPropertiesFromMapPattern
import org.neo4j.cypher.internal.ir.SetRelationshipPropertiesPattern
import org.neo4j.cypher.internal.ir.SetRelationshipPropertyPattern
import org.neo4j.cypher.internal.ir.ShortestRelationshipPattern
import org.neo4j.cypher.internal.ir.SimpleMutatingPattern
import org.neo4j.cypher.internal.ir.SinglePlannerQuery
import org.neo4j.cypher.internal.ir.UnionQuery
import org.neo4j.cypher.internal.ir.UnwindProjection
import org.neo4j.cypher.internal.ir.VarPatternLength
import org.neo4j.cypher.internal.ir.VariableGrouping
import org.neo4j.cypher.internal.ir.ast.IRExpression
import org.neo4j.cypher.internal.ir.ordering
import org.neo4j.cypher.internal.ir.ordering.InterestingOrder
import org.neo4j.cypher.internal.ir.ordering.ProvidedOrder
import org.neo4j.cypher.internal.ir.ordering.ProvidedOrderFactory
import org.neo4j.cypher.internal.logical.plans
import org.neo4j.cypher.internal.logical.plans.Aggregation
import org.neo4j.cypher.internal.logical.plans.AllNodesScan
import org.neo4j.cypher.internal.logical.plans.AntiConditionalApply
import org.neo4j.cypher.internal.logical.plans.AntiSemiApply
import org.neo4j.cypher.internal.logical.plans.Apply
import org.neo4j.cypher.internal.logical.plans.Argument
import org.neo4j.cypher.internal.logical.plans.AssertSameNode
import org.neo4j.cypher.internal.logical.plans.AssertSameRelationship
import org.neo4j.cypher.internal.logical.plans.CartesianProduct
import org.neo4j.cypher.internal.logical.plans.ColumnOrder
import org.neo4j.cypher.internal.logical.plans.ConditionalApply
import org.neo4j.cypher.internal.logical.plans.DeleteNode
import org.neo4j.cypher.internal.logical.plans.DeletePath
import org.neo4j.cypher.internal.logical.plans.DeleteRelationship
import org.neo4j.cypher.internal.logical.plans.DetachDeleteExpression
import org.neo4j.cypher.internal.logical.plans.DetachDeleteNode
import org.neo4j.cypher.internal.logical.plans.DetachDeletePath
import org.neo4j.cypher.internal.logical.plans.DirectedAllRelationshipsScan
import org.neo4j.cypher.internal.logical.plans.DirectedRelationshipByElementIdSeek
import org.neo4j.cypher.internal.logical.plans.DirectedRelationshipByIdSeek
import org.neo4j.cypher.internal.logical.plans.DirectedRelationshipIndexContainsScan
import org.neo4j.cypher.internal.logical.plans.DirectedRelationshipIndexEndsWithScan
import org.neo4j.cypher.internal.logical.plans.DirectedRelationshipIndexScan
import org.neo4j.cypher.internal.logical.plans.DirectedRelationshipIndexSeek
import org.neo4j.cypher.internal.logical.plans.DirectedRelationshipTypeScan
import org.neo4j.cypher.internal.logical.plans.DirectedRelationshipUniqueIndexSeek
import org.neo4j.cypher.internal.logical.plans.DirectedUnionRelationshipTypesScan
import org.neo4j.cypher.internal.logical.plans.Distinct
import org.neo4j.cypher.internal.logical.plans.Eager
import org.neo4j.cypher.internal.logical.plans.EmptyResult
import org.neo4j.cypher.internal.logical.plans.ErrorPlan
import org.neo4j.cypher.internal.logical.plans.ExhaustiveLimit
import org.neo4j.cypher.internal.logical.plans.Expand
import org.neo4j.cypher.internal.logical.plans.Expand.ExpansionMode
import org.neo4j.cypher.internal.logical.plans.Expand.VariablePredicate
import org.neo4j.cypher.internal.logical.plans.FindShortestPaths
import org.neo4j.cypher.internal.logical.plans.Foreach
import org.neo4j.cypher.internal.logical.plans.ForeachApply
import org.neo4j.cypher.internal.logical.plans.IndexOrder
import org.neo4j.cypher.internal.logical.plans.IndexOrderAscending
import org.neo4j.cypher.internal.logical.plans.IndexOrderDescending
import org.neo4j.cypher.internal.logical.plans.IndexOrderNone
import org.neo4j.cypher.internal.logical.plans.IndexedProperty
import org.neo4j.cypher.internal.logical.plans.Input
import org.neo4j.cypher.internal.logical.plans.IntersectionNodeByLabelsScan
import org.neo4j.cypher.internal.logical.plans.LeftOuterHashJoin
import org.neo4j.cypher.internal.logical.plans.LegacyFindShortestPaths
import org.neo4j.cypher.internal.logical.plans.LetAntiSemiApply
import org.neo4j.cypher.internal.logical.plans.LetSelectOrAntiSemiApply
import org.neo4j.cypher.internal.logical.plans.LetSelectOrSemiApply
import org.neo4j.cypher.internal.logical.plans.LetSemiApply
import org.neo4j.cypher.internal.logical.plans.Limit
import org.neo4j.cypher.internal.logical.plans.LoadCSV
import org.neo4j.cypher.internal.logical.plans.LogicalPlan
import org.neo4j.cypher.internal.logical.plans.LogicalPlanToPlanBuilderString
import org.neo4j.cypher.internal.logical.plans.Merge
import org.neo4j.cypher.internal.logical.plans.NodeByElementIdSeek
import org.neo4j.cypher.internal.logical.plans.NodeByIdSeek
import org.neo4j.cypher.internal.logical.plans.NodeByLabelScan
import org.neo4j.cypher.internal.logical.plans.NodeCountFromCountStore
import org.neo4j.cypher.internal.logical.plans.NodeHashJoin
import org.neo4j.cypher.internal.logical.plans.NodeIndexContainsScan
import org.neo4j.cypher.internal.logical.plans.NodeIndexEndsWithScan
import org.neo4j.cypher.internal.logical.plans.NodeIndexScan
import org.neo4j.cypher.internal.logical.plans.NodeIndexSeek
import org.neo4j.cypher.internal.logical.plans.NodeLogicalLeafPlan
import org.neo4j.cypher.internal.logical.plans.NodeUniqueIndexSeek
import org.neo4j.cypher.internal.logical.plans.Optional
import org.neo4j.cypher.internal.logical.plans.OrderedAggregation
import org.neo4j.cypher.internal.logical.plans.OrderedDistinct
import org.neo4j.cypher.internal.logical.plans.OrderedUnion
import org.neo4j.cypher.internal.logical.plans.PartialSort
import org.neo4j.cypher.internal.logical.plans.ProcedureCall
import org.neo4j.cypher.internal.logical.plans.ProduceResult
import org.neo4j.cypher.internal.logical.plans.ProjectEndpoints
import org.neo4j.cypher.internal.logical.plans.Projection
import org.neo4j.cypher.internal.logical.plans.QueryExpression
import org.neo4j.cypher.internal.logical.plans.RelationshipCountFromCountStore
import org.neo4j.cypher.internal.logical.plans.RelationshipLogicalLeafPlan
import org.neo4j.cypher.internal.logical.plans.RemoveLabels
import org.neo4j.cypher.internal.logical.plans.ResolvedCall
import org.neo4j.cypher.internal.logical.plans.RightOuterHashJoin
import org.neo4j.cypher.internal.logical.plans.RollUpApply
import org.neo4j.cypher.internal.logical.plans.SeekableArgs
import org.neo4j.cypher.internal.logical.plans.SelectOrAntiSemiApply
import org.neo4j.cypher.internal.logical.plans.SelectOrSemiApply
import org.neo4j.cypher.internal.logical.plans.Selection
import org.neo4j.cypher.internal.logical.plans.Selection.LabelAndRelTypeInfo
import org.neo4j.cypher.internal.logical.plans.SemiApply
import org.neo4j.cypher.internal.logical.plans.SetLabels
import org.neo4j.cypher.internal.logical.plans.SetNodeProperties
import org.neo4j.cypher.internal.logical.plans.SetNodePropertiesFromMap
import org.neo4j.cypher.internal.logical.plans.SetNodeProperty
import org.neo4j.cypher.internal.logical.plans.SetProperties
import org.neo4j.cypher.internal.logical.plans.SetPropertiesFromMap
import org.neo4j.cypher.internal.logical.plans.SetProperty
import org.neo4j.cypher.internal.logical.plans.SetRelationshipProperties
import org.neo4j.cypher.internal.logical.plans.SetRelationshipPropertiesFromMap
import org.neo4j.cypher.internal.logical.plans.SetRelationshipProperty
import org.neo4j.cypher.internal.logical.plans.ShowConstraints
import org.neo4j.cypher.internal.logical.plans.ShowFunctions
import org.neo4j.cypher.internal.logical.plans.ShowIndexes
import org.neo4j.cypher.internal.logical.plans.ShowProcedures
import org.neo4j.cypher.internal.logical.plans.ShowSettings
import org.neo4j.cypher.internal.logical.plans.ShowTransactions
import org.neo4j.cypher.internal.logical.plans.Skip
import org.neo4j.cypher.internal.logical.plans.Sort
import org.neo4j.cypher.internal.logical.plans.SubqueryForeach
import org.neo4j.cypher.internal.logical.plans.TerminateTransactions
import org.neo4j.cypher.internal.logical.plans.Top
import org.neo4j.cypher.internal.logical.plans.Top1WithTies
import org.neo4j.cypher.internal.logical.plans.Trail
import org.neo4j.cypher.internal.logical.plans.TransactionApply
import org.neo4j.cypher.internal.logical.plans.TransactionForeach
import org.neo4j.cypher.internal.logical.plans.TriadicSelection
import org.neo4j.cypher.internal.logical.plans.UndirectedAllRelationshipsScan
import org.neo4j.cypher.internal.logical.plans.UndirectedRelationshipByElementIdSeek
import org.neo4j.cypher.internal.logical.plans.UndirectedRelationshipByIdSeek
import org.neo4j.cypher.internal.logical.plans.UndirectedRelationshipIndexContainsScan
import org.neo4j.cypher.internal.logical.plans.UndirectedRelationshipIndexEndsWithScan
import org.neo4j.cypher.internal.logical.plans.UndirectedRelationshipIndexScan
import org.neo4j.cypher.internal.logical.plans.UndirectedRelationshipIndexSeek
import org.neo4j.cypher.internal.logical.plans.UndirectedRelationshipTypeScan
import org.neo4j.cypher.internal.logical.plans.UndirectedRelationshipUniqueIndexSeek
import org.neo4j.cypher.internal.logical.plans.UndirectedUnionRelationshipTypesScan
import org.neo4j.cypher.internal.logical.plans.Union
import org.neo4j.cypher.internal.logical.plans.UnionNodeByLabelsScan
import org.neo4j.cypher.internal.logical.plans.UnwindCollection
import org.neo4j.cypher.internal.logical.plans.UpdatingPlan
import org.neo4j.cypher.internal.logical.plans.ValueHashJoin
import org.neo4j.cypher.internal.logical.plans.VarExpand
import org.neo4j.cypher.internal.macros.AssertMacros
import org.neo4j.cypher.internal.macros.AssertMacros.checkOnlyWhenAssertionsAreEnabled
import org.neo4j.cypher.internal.planner.spi.IndexDescriptor.IndexType
import org.neo4j.cypher.internal.planner.spi.PlanningAttributes
import org.neo4j.cypher.internal.util.AssertionRunner
import org.neo4j.cypher.internal.util.Foldable.FoldableAny
import org.neo4j.cypher.internal.util.InputPosition
import org.neo4j.cypher.internal.util.UpperBound
import org.neo4j.cypher.internal.util.attribution.Attributes
import org.neo4j.cypher.internal.util.attribution.IdGen
import org.neo4j.exceptions.ExhaustiveShortestPathForbiddenException
import org.neo4j.exceptions.InternalException

import scala.collection.immutable.ListSet

/*
 * The responsibility of this class is to produce the correct solved PlannerQuery when creating logical plans.
 * No other functionality or logic should live here - this is supposed to be a very simple class that does not need
 * much testing
 */
case class LogicalPlanProducer(
  cardinalityModel: CardinalityModel,
  planningAttributes: PlanningAttributes,
  idGen: IdGen
) {

  implicit val implicitIdGen: IdGen = idGen
  private val solveds = planningAttributes.solveds
  private val cardinalities = planningAttributes.cardinalities
  private val providedOrders = planningAttributes.providedOrders
  private val leveragedOrders = planningAttributes.leveragedOrders
  private val labelAndRelTypeInfos = planningAttributes.labelAndRelTypeInfos

  private val attributesWithoutSolveds =
    planningAttributes.asAttributes(idGen).without(solveds, planningAttributes.effectiveCardinalities)

  /**
   * This object is simply to group methods that are used by the [[SubqueryExpressionSolver]], and thus do not need to update `solveds`
   */
  object ForSubqueryExpressionSolver {

    def planArgument(argumentIds: Set[String], context: LogicalPlanningContext): LogicalPlan = {
      annotate(Argument(argumentIds), SinglePlannerQuery.empty, ProvidedOrder.empty, context)
    }

    def planApply(left: LogicalPlan, right: LogicalPlan, context: LogicalPlanningContext): LogicalPlan = {
      // If the LHS has duplicate values, we cannot guarantee any added order from the RHS
      val providedOrder = providedOrders.get(left.id).fromLeft
      // The RHS is the leaf plan we are wrapping under an apply in order to solve the pattern expression.
      // It has the correct solved
      val solved = solveds.get(right.id)
      annotate(Apply(left, right), solved, providedOrder, context)
    }

    def planRollup(
      lhs: LogicalPlan,
      rhs: LogicalPlan,
      collectionName: String,
      variableToCollect: String,
      context: LogicalPlanningContext
    ): LogicalPlan = {
      // The LHS is either the plan we're building on top of, with the correct solved or it is the result of [[planArgument]].
      // The RHS is the sub-query
      val solved = solveds.get(lhs.id)
      annotate(
        RollUpApply(lhs, rhs, collectionName, variableToCollect),
        solved,
        providedOrders.get(lhs.id).fromLeft,
        context
      )
    }

    def planCountExpressionApply(
      lhs: LogicalPlan,
      rhs: LogicalPlan,
      context: LogicalPlanningContext
    ): LogicalPlan = {
      val solved = solveds.get(lhs.id)
      annotate(Apply(lhs, rhs), solved, providedOrders.get(lhs.id).fromLeft, context)
    }
  }

  def solvePredicate(plan: LogicalPlan, solvedExpression: Expression): LogicalPlan = {
    // Keep other attributes but change solved
    val keptAttributes = Attributes(idGen, cardinalities, providedOrders, leveragedOrders, labelAndRelTypeInfos)
    val newPlan = plan.copyPlanWithIdGen(keptAttributes.copy(plan.id))
    val solvedPlannerQuery =
      solveds.get(plan.id).asSinglePlannerQuery.amendQueryGraph(_.addPredicates(solvedExpression))
    solveds.set(newPlan.id, solvedPlannerQuery)
    newPlan
  }

  def solvePredicateInHorizon(plan: LogicalPlan, solvedExpression: Expression): LogicalPlan = {
    // Keep other attributes but change solved
    val keptAttributes = Attributes(idGen, cardinalities, providedOrders, leveragedOrders, labelAndRelTypeInfos)
    val newPlan = plan.copyPlanWithIdGen(keptAttributes.copy(plan.id))
    val solvedPlannerQuery = solveds.get(plan.id).asSinglePlannerQuery.updateTailOrSelf(_.updateHorizon {
      case horizon: QueryProjection => horizon.addPredicates(solvedExpression)
      case horizon                  => horizon
    })
    solveds.set(newPlan.id, solvedPlannerQuery)
    newPlan
  }

  def planAllNodesScan(idName: String, argumentIds: Set[String], context: LogicalPlanningContext): LogicalPlan = {
    val solved =
      RegularSinglePlannerQuery(queryGraph = QueryGraph(argumentIds = argumentIds, patternNodes = Set(idName)))
    annotate(AllNodesScan(idName, argumentIds), solved, ProvidedOrder.empty, context)
  }

  /**
   * @param idName             the name of the relationship variable
   * @param patternForLeafPlan the pattern to use for the leaf plan
   * @param originalPattern    the original pattern, as it appears in the query graph
   * @param hiddenSelections   selections that make the leaf plan solve the originalPattern instead.
   *                           Must not contain any pattern expressions or pattern comprehensions.
   */
  def planAllRelationshipsScan(
    idName: String,
    patternForLeafPlan: PatternRelationship,
    originalPattern: PatternRelationship,
    hiddenSelections: Seq[Expression],
    argumentIds: Set[String],
    context: LogicalPlanningContext
  ): LogicalPlan = {
    require(patternForLeafPlan.types.isEmpty)
    def planLeaf: LogicalPlan = {
      val (firstNode, secondNode) = patternForLeafPlan.inOrder
      val solved =
        RegularSinglePlannerQuery(queryGraph =
          QueryGraph(
            argumentIds = argumentIds,
            patternNodes = Set(firstNode, secondNode),
            patternRelationships = Set(patternForLeafPlan)
          )
        )

      val leafPlan =
        if (patternForLeafPlan.dir == BOTH) {
          UndirectedAllRelationshipsScan(idName, firstNode, secondNode, argumentIds)
        } else {
          DirectedAllRelationshipsScan(idName, firstNode, secondNode, argumentIds)
        }
      annotate(leafPlan, solved, ProvidedOrder.empty, context)
    }

    planHiddenSelectionIfNeeded(planLeaf, hiddenSelections, context, originalPattern)
  }

  /**
   * @param idName             the name of the relationship variable
   * @param relType            the relType to scan
   * @param patternForLeafPlan the pattern to use for the leaf plan
   * @param originalPattern    the original pattern, as it appears in the query graph
   * @param hiddenSelections   selections that make the leaf plan solve the originalPattern instead.
   *                           Must not contain any pattern expressions or pattern comprehensions.
   */
  def planRelationshipByTypeScan(
    idName: String,
    relType: RelTypeName,
    patternForLeafPlan: PatternRelationship,
    originalPattern: PatternRelationship,
    hiddenSelections: Seq[Expression],
    solvedHint: Option[UsingScanHint],
    argumentIds: Set[String],
    providedOrder: ProvidedOrder,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    def planLeaf: LogicalPlan = {
      val (firstNode, secondNode) = patternForLeafPlan.inOrder
      val leafPlan: RelationshipLogicalLeafPlan =
        if (patternForLeafPlan.dir == BOTH) {
          UndirectedRelationshipTypeScan(
            idName,
            firstNode,
            relType,
            secondNode,
            argumentIds,
            toIndexOrder(providedOrder)
          )
        } else {
          DirectedRelationshipTypeScan(idName, firstNode, relType, secondNode, argumentIds, toIndexOrder(providedOrder))
        }

      annotateRelationshipLeafPlan(
        leafPlan,
        patternForLeafPlan,
        Seq.empty,
        solvedHint,
        argumentIds,
        providedOrder,
        context
      )
    }

    planHiddenSelectionIfNeeded(planLeaf, hiddenSelections, context, originalPattern)
  }

  /**
   * @param idName             the name of the relationship variable
   * @param relTypes           the relTypes to scan
   * @param patternForLeafPlan the pattern to use for the leaf plan
   * @param originalPattern    the original pattern, as it appears in the query graph
   * @param hiddenSelections   selections that make the leaf plan solve the originalPattern instead.
   *                           Must not contain any pattern expressions or pattern comprehensions.
   */
  def planUnionRelationshipByTypeScan(
    idName: String,
    relTypes: Seq[RelTypeName],
    patternForLeafPlan: PatternRelationship,
    originalPattern: PatternRelationship,
    hiddenSelections: Seq[Expression],
    solvedHints: Seq[UsingScanHint],
    argumentIds: Set[String],
    providedOrder: ProvidedOrder,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    def planLeaf: LogicalPlan = {
      val (firstNode, secondNode) = patternForLeafPlan.inOrder
      val leafPlan: RelationshipLogicalLeafPlan =
        if (patternForLeafPlan.dir == BOTH) {
          UndirectedUnionRelationshipTypesScan(
            idName,
            firstNode,
            relTypes,
            secondNode,
            argumentIds,
            toIndexOrder(providedOrder)
          )
        } else {
          DirectedUnionRelationshipTypesScan(
            idName,
            firstNode,
            relTypes,
            secondNode,
            argumentIds,
            toIndexOrder(providedOrder)
          )
        }

      annotateRelationshipLeafPlan(
        leafPlan,
        patternForLeafPlan,
        Seq.empty,
        solvedHints,
        argumentIds,
        providedOrder,
        context
      )
    }

    planHiddenSelectionIfNeeded(planLeaf, hiddenSelections, context, originalPattern)
  }

  def planRelationshipIndexScan(
    idName: String,
    relationshipType: RelationshipTypeToken,
    patternForLeafPlan: PatternRelationship,
    originalPattern: PatternRelationship,
    properties: Seq[IndexedProperty],
    solvedPredicates: Seq[Expression] = Seq.empty,
    solvedHint: Option[UsingIndexHint] = None,
    hiddenSelections: Seq[Expression],
    argumentIds: Set[String],
    providedOrder: ProvidedOrder,
    indexOrder: IndexOrder,
    context: LogicalPlanningContext,
    indexType: IndexType
  ): LogicalPlan = {
    def planLeaf: LogicalPlan = {
      val leafPlan =
        if (patternForLeafPlan.dir == BOTH) {
          UndirectedRelationshipIndexScan(
            idName,
            patternForLeafPlan.inOrder._1,
            patternForLeafPlan.inOrder._2,
            relationshipType,
            properties,
            argumentIds,
            indexOrder,
            indexType.toPublicApi
          )
        } else {
          DirectedRelationshipIndexScan(
            idName,
            patternForLeafPlan.inOrder._1,
            patternForLeafPlan.inOrder._2,
            relationshipType,
            properties,
            argumentIds,
            indexOrder,
            indexType.toPublicApi
          )
        }

      annotateRelationshipLeafPlan(
        leafPlan,
        patternForLeafPlan,
        solvedPredicates,
        solvedHint,
        argumentIds,
        providedOrder,
        context
      )
    }

    planHiddenSelectionIfNeeded(planLeaf, hiddenSelections, context, originalPattern)
  }

  def planRelationshipIndexStringSearchScan(
    idName: String,
    relationshipType: RelationshipTypeToken,
    patternForLeafPlan: PatternRelationship,
    originalPattern: PatternRelationship,
    properties: Seq[IndexedProperty],
    stringSearchMode: StringSearchMode,
    solvedPredicates: Seq[Expression] = Seq.empty,
    solvedHint: Option[UsingIndexHint] = None,
    hiddenSelections: Seq[Expression],
    valueExpr: Expression,
    argumentIds: Set[String],
    providedOrder: ProvidedOrder,
    indexOrder: IndexOrder,
    context: LogicalPlanningContext,
    indexType: IndexType
  ): LogicalPlan = {
    def planLeaf = {
      val solver = SubqueryExpressionSolver.solverForLeafPlan(argumentIds, context)
      val rewrittenValueExpr = solver.solve(valueExpr)
      val newArguments = solver.newArguments

      val planTemplate = (patternForLeafPlan.dir, stringSearchMode) match {
        case (SemanticDirection.BOTH, ContainsSearchMode) =>
          UndirectedRelationshipIndexContainsScan(_, _, _, _, _, _, _, _, _)
        case (SemanticDirection.BOTH, EndsWithSearchMode) =>
          UndirectedRelationshipIndexEndsWithScan(_, _, _, _, _, _, _, _, _)
        case (SemanticDirection.INCOMING | SemanticDirection.OUTGOING, ContainsSearchMode) =>
          DirectedRelationshipIndexContainsScan(_, _, _, _, _, _, _, _, _)
        case (SemanticDirection.INCOMING | SemanticDirection.OUTGOING, EndsWithSearchMode) =>
          DirectedRelationshipIndexEndsWithScan(_, _, _, _, _, _, _, _, _)
      }

      val leafPlan = planTemplate(
        idName,
        patternForLeafPlan.inOrder._1,
        patternForLeafPlan.inOrder._2,
        relationshipType,
        properties.head,
        rewrittenValueExpr,
        argumentIds ++ newArguments,
        indexOrder,
        indexType.toPublicApi
      )

      solver.rewriteLeafPlan {
        annotateRelationshipLeafPlan(
          leafPlan,
          patternForLeafPlan,
          solvedPredicates,
          solvedHint,
          argumentIds,
          providedOrder,
          context
        )
      }
    }

    planHiddenSelectionIfNeeded(planLeaf, hiddenSelections, context, originalPattern)
  }

  def planRelationshipIndexSeek(
    idName: String,
    typeToken: RelationshipTypeToken,
    properties: Seq[IndexedProperty],
    valueExpr: QueryExpression[Expression],
    argumentIds: Set[String],
    indexOrder: IndexOrder,
    patternForLeafPlan: PatternRelationship,
    originalPattern: PatternRelationship,
    solvedPredicates: Seq[Expression],
    solvedHint: Option[UsingIndexHint],
    hiddenSelections: Seq[Expression],
    providedOrder: ProvidedOrder,
    context: LogicalPlanningContext,
    indexType: IndexType,
    unique: Boolean
  ): LogicalPlan = {

    def planLeaf = {
      val solver = SubqueryExpressionSolver.solverForLeafPlan(argumentIds, context)
      val rewrittenValueExpr = valueExpr.map(solver.solve(_))
      val newArguments = solver.newArguments

      val leafPlan =
        if (patternForLeafPlan.dir == SemanticDirection.BOTH) {
          val makeUndirected =
            if (unique)
              UndirectedRelationshipUniqueIndexSeek.apply _
            else
              UndirectedRelationshipIndexSeek.apply _

          makeUndirected(
            idName,
            patternForLeafPlan.left,
            patternForLeafPlan.right,
            typeToken,
            properties,
            rewrittenValueExpr,
            argumentIds ++ newArguments,
            indexOrder,
            indexType.toPublicApi
          )
        } else {
          val makeDirected =
            if (unique)
              DirectedRelationshipUniqueIndexSeek.apply _
            else
              DirectedRelationshipIndexSeek.apply _

          makeDirected(
            idName,
            patternForLeafPlan.inOrder._1,
            patternForLeafPlan.inOrder._2,
            typeToken,
            properties,
            rewrittenValueExpr,
            argumentIds ++ newArguments,
            indexOrder,
            indexType.toPublicApi
          )
        }

      solver.rewriteLeafPlan {
        annotateRelationshipLeafPlan(
          leafPlan,
          patternForLeafPlan,
          solvedPredicates,
          solvedHint,
          argumentIds,
          providedOrder,
          context
        )
      }
    }

    planHiddenSelectionIfNeeded(planLeaf, hiddenSelections, context, originalPattern)
  }

  /**
   * @param idName             the name of the relationship variable
   * @param patternForLeafPlan the pattern to use for the leaf plan
   * @param originalPattern    the original pattern, as it appears in the query graph
   * @param hiddenSelections   selections that make the leaf plan solve the originalPattern instead.
   *                           Must not contain any pattern expressions or pattern comprehensions.
   */
  def planRelationshipByIdSeek(
    idName: String,
    relIds: SeekableArgs,
    patternForLeafPlan: PatternRelationship,
    originalPattern: PatternRelationship,
    hiddenSelections: Seq[Expression],
    argumentIds: Set[String],
    solvedPredicates: Seq[Expression] = Seq.empty,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    doPlanRelationshipByIdSeek(
      UndirectedRelationshipByIdSeek.apply,
      DirectedRelationshipByIdSeek.apply,
      idName,
      relIds,
      patternForLeafPlan,
      originalPattern,
      hiddenSelections,
      argumentIds,
      solvedPredicates,
      context
    )
  }

  def planRelationshipByElementIdSeek(
    idName: String,
    relIds: SeekableArgs,
    patternForLeafPlan: PatternRelationship,
    originalPattern: PatternRelationship,
    hiddenSelections: Seq[Expression],
    argumentIds: Set[String],
    solvedPredicates: Seq[Expression] = Seq.empty,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    doPlanRelationshipByIdSeek(
      UndirectedRelationshipByElementIdSeek.apply,
      DirectedRelationshipByElementIdSeek.apply,
      idName,
      relIds,
      patternForLeafPlan,
      originalPattern,
      hiddenSelections,
      argumentIds,
      solvedPredicates,
      context
    )
  }

  private def doPlanRelationshipByIdSeek(
    makeUndirected: (String, SeekableArgs, String, String, Set[String]) => RelationshipLogicalLeafPlan,
    makeDirected: (String, SeekableArgs, String, String, Set[String]) => RelationshipLogicalLeafPlan,
    idName: String,
    relIds: SeekableArgs,
    patternForLeafPlan: PatternRelationship,
    originalPattern: PatternRelationship,
    hiddenSelections: Seq[Expression],
    argumentIds: Set[String],
    solvedPredicates: Seq[Expression],
    context: LogicalPlanningContext
  ): LogicalPlan = {
    def planLeaf: LogicalPlan = {
      val (firstNode, secondNode) = patternForLeafPlan.inOrder
      val solver = SubqueryExpressionSolver.solverForLeafPlan(argumentIds, context)
      val rewrittenRelIds = relIds.mapValues(solver.solve(_))
      val newArguments = solver.newArguments

      val leafPlan =
        if (patternForLeafPlan.dir == BOTH) {
          makeUndirected(idName, rewrittenRelIds, firstNode, secondNode, argumentIds ++ newArguments)
        } else {
          makeDirected(idName, rewrittenRelIds, firstNode, secondNode, argumentIds ++ newArguments)
        }

      solver.rewriteLeafPlan {
        annotateRelationshipLeafPlan(
          leafPlan,
          patternForLeafPlan,
          solvedPredicates,
          None,
          argumentIds,
          ProvidedOrder.empty,
          context
        )
      }
    }

    planHiddenSelectionIfNeeded(planLeaf, hiddenSelections, context, originalPattern)
  }

  private def annotateRelationshipLeafPlan(
    leafPlan: RelationshipLogicalLeafPlan,
    patternForLeafPlan: PatternRelationship,
    solvedPredicates: Seq[Expression],
    solvedHint: IterableOnce[Hint],
    argumentIds: Set[String],
    providedOrder: ProvidedOrder,
    context: LogicalPlanningContext
  ) = {
    val solved = RegularSinglePlannerQuery(queryGraph =
      QueryGraph.empty
        .addPatternRelationship(patternForLeafPlan)
        .addPredicates(solvedPredicates: _*)
        .addHints(solvedHint)
        .addArgumentIds(argumentIds.toIndexedSeq)
    )

    annotate(leafPlan, solved, providedOrder, context)
  }

  private def computeBatchSize(maybeBatchSize: Option[Expression]): Expression = {
    maybeBatchSize match {
      case Some(batchSize) => batchSize
      case None => SignedDecimalIntegerLiteral(TransactionForeach.defaultBatchSize.toString)(InputPosition.NONE)
    }
  }

  private def computeErrorBehaviour(maybeErrorParams: Option[InTransactionsErrorParameters])
    : InTransactionsOnErrorBehaviour = {
    maybeErrorParams.map(_.behaviour).getOrElse(TransactionForeach.defaultOnErrorBehaviour)
  }

  private def computeMaybeReportAs(maybeReportParams: Option[InTransactionsReportParameters]): Option[String] = {
    maybeReportParams.map(_.reportAs.name)
  }

  /**
   * Plan a selection on `hiddenSelections` but, in the solveds, pretend to solve only the predicates of the leaf plan and `originalPattern` instead of the leaf plan's pattern.
   * @param source the source leaf plan
   * @param hiddenSelections the selections to test in this operator
   * @param context planning context
   * @param solvedPattern the pattern we will claim to have solved
   * @return hidden selection on top of source plan
   */
  def planHiddenSelectionIfNeeded(
    source: LogicalPlan,
    hiddenSelections: Seq[Expression],
    context: LogicalPlanningContext,
    solvedPattern: PatternRelationship
  ): LogicalPlan = {
    if (hiddenSelections.isEmpty) {
      source
    } else {
      val solved =
        solveds.get(source.id).asSinglePlannerQuery.updateTailOrSelf(_.amendQueryGraph(_.withPattern(solvedPattern)))
      planSelectionWithGivenSolved(source, hiddenSelections, solved, context)
    }
  }

  def planApply(left: LogicalPlan, right: LogicalPlan, context: LogicalPlanningContext): LogicalPlan = {
    // We don't want to keep the arguments that this Apply is inserting on the RHS, so we remove them here.
    val rhsSolved =
      solveds.get(right.id).asSinglePlannerQuery.updateTailOrSelf(_.amendQueryGraph(_.withArgumentIds(Set.empty)))
    val solved = solveds.get(left.id).asSinglePlannerQuery ++ rhsSolved
    val plan = Apply(left, right)
    val providedOrder = providedOrderOfApply(left, right, context.settings.executionModel)
    annotate(plan, solved, providedOrder, context)
  }

  def planSubquery(
    left: LogicalPlan,
    right: LogicalPlan,
    context: LogicalPlanningContext,
    correlated: Boolean,
    yielding: Boolean,
    inTransactionsParameters: Option[InTransactionsParameters]
  ): LogicalPlan = {
    val solvedLeft = solveds.get(left.id)
    val solvedRight = solveds.get(right.id)
    val solved = solvedLeft.asSinglePlannerQuery.updateTailOrSelf(_.withHorizon(CallSubqueryHorizon(
      solvedRight,
      correlated,
      yielding,
      inTransactionsParameters
    )))

    val plan =
      if (yielding) {
        inTransactionsParameters match {
          case Some(InTransactionsParameters(batchParams, errorParams, reportParams)) =>
            TransactionApply(
              left,
              right,
              computeBatchSize(batchParams.map(_.batchSize)),
              computeErrorBehaviour(errorParams),
              computeMaybeReportAs(reportParams)
            )
          case None =>
            if (!correlated && solvedRight.readOnly) {
              CartesianProduct(left, right, fromSubquery = true)
            } else {
              Apply(left, right, fromSubquery = true)
            }
        }
      } else {
        inTransactionsParameters match {
          case Some(InTransactionsParameters(batchParams, errorParams, reportParams)) =>
            TransactionForeach(
              left,
              right,
              computeBatchSize(batchParams.map(_.batchSize)),
              computeErrorBehaviour(errorParams),
              computeMaybeReportAs(reportParams)
            )
          case None => SubqueryForeach(left, right)
        }
      }

    val providedOrder = providedOrderOfApply(left, right, context.settings.executionModel)
    annotate(plan, solved, providedOrder, context)
  }

  def planTailApply(left: LogicalPlan, right: LogicalPlan, context: LogicalPlanningContext): LogicalPlan = {
    val solved =
      solveds.get(left.id).asSinglePlannerQuery.updateTailOrSelf(_.withTail(solveds.get(right.id).asSinglePlannerQuery))
    val providedOrder = providedOrderOfApply(left, right, context.settings.executionModel)
    annotate(Apply(left, right), solved, providedOrder, context)
  }

  def planInputApply(
    left: LogicalPlan,
    right: LogicalPlan,
    symbols: Seq[Variable],
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(right.id).asSinglePlannerQuery.withInput(symbols)
    // If the LHS has duplicate values, we cannot guarantee any added order from the RHS
    val providedOrder = providedOrders.get(left.id).fromLeft
    annotate(Apply(left, right), solved, providedOrder, context)
  }

  def planCartesianProduct(left: LogicalPlan, right: LogicalPlan, context: LogicalPlanningContext): LogicalPlan = {
    val solved: SinglePlannerQuery =
      solveds.get(left.id).asSinglePlannerQuery ++ solveds.get(right.id).asSinglePlannerQuery
    val providedOrder = providedOrderOfApply(left, right, context.settings.executionModel)
    annotate(CartesianProduct(left, right), solved, providedOrder, context)
  }

  def planSimpleExpand(
    left: LogicalPlan,
    from: String,
    to: String,
    pattern: PatternRelationship,
    mode: ExpansionMode,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val dir = pattern.directionRelativeTo(from)
    val solved = solveds.get(left.id).asSinglePlannerQuery.amendQueryGraph(_.addPatternRelationship(pattern))
    val providedOrder = providedOrders.get(left.id).fromLeft
    annotate(Expand(left, from, dir, pattern.types, to, pattern.name, mode), solved, providedOrder, context)
  }

  /**
   * Assumes that qpp.isSimple is true
   * @param qpp A simple QuantifiedPathPattern
   * @return a PatternRelationship that represents the qpp
   */
  def simpleQuantifiedPathPatternToPatternRelationship(qpp: QuantifiedPathPattern): PatternRelationship = {
    val max = qpp.repetition.max match {
      case UpperBound.Unlimited  => None
      case UpperBound.Limited(n) => Some(n.toInt)
    }

    qpp.patternRelationships.head.copy(
      nodes = (qpp.leftBinding.outer, qpp.rightBinding.outer),
      name = qpp.relationshipVariableGroupings.head.groupName,
      length = VarPatternLength(qpp.repetition.min.toInt, max)
    )
  }

  def planVarExpand(
    source: LogicalPlan,
    from: String,
    to: String,
    nodeConnection: NodeConnection,
    relationshipPredicates: ListSet[VariablePredicate],
    nodePredicates: ListSet[VariablePredicate],
    solvedPredicates: ListSet[Expression],
    mode: ExpansionMode,
    context: LogicalPlanningContext
  ): LogicalPlan = {

    val (pattern, solvedConnection) = nodeConnection match {
      case qpp: QuantifiedPathPattern if qpp.isSimple =>
        val innerPatternRelationship = qpp.patternRelationships.head
        val innerNodes = Set(innerPatternRelationship.nodes._1, innerPatternRelationship.nodes._2)

        // It should be safe to mark 'qpp' as solved at this point.
        // However, given how much information that could actually be contained in a QuantifiedPathPattern, it's safer to construct a "solved" by consciously
        // extracting the specific elements that we _know_ are solved by the VarExpand.
        val solvedQpp = QuantifiedPathPattern(
          leftBinding = qpp.leftBinding,
          rightBinding = qpp.rightBinding,
          repetition = qpp.repetition,
          patternRelationships = List(innerPatternRelationship),
          patternNodes = innerNodes,
          argumentIds = qpp.argumentIds,
          selections = Selections.empty,
          nodeVariableGroupings = Set.empty,
          relationshipVariableGroupings = qpp.relationshipVariableGroupings
        )

        (simpleQuantifiedPathPatternToPatternRelationship(qpp), solvedQpp)

      case _: QuantifiedPathPattern =>
        throw new InternalException("Tried to solve a non-simple quantified path pattern with a VarExpand")
      case pr: PatternRelationship => (pr, pr)
    }

    val dir = pattern.directionRelativeTo(from)

    pattern.length match {
      case l: VarPatternLength =>
        val projectedDir = projectedDirection(pattern, from, dir)

        val solved = solveds.get(source.id).asSinglePlannerQuery.amendQueryGraph(_
          .addNodeConnection(solvedConnection)
          .addPredicates(solvedPredicates.toSeq: _*))

        val (rewrittenRelationshipPredicates, rewrittenNodePredicates, rewrittenSource) =
          solveSubqueryExpressionsForExtractedPredicates(source, nodePredicates, relationshipPredicates, context)
        annotate(
          VarExpand(
            source = rewrittenSource,
            from = from,
            dir = dir,
            projectedDir = projectedDir,
            types = pattern.types,
            to = to,
            relName = pattern.name,
            length = l,
            mode = mode,
            nodePredicates = rewrittenNodePredicates.toSeq,
            relationshipPredicates = rewrittenRelationshipPredicates.toSeq
          ),
          solved,
          providedOrders.get(source.id).fromLeft,
          context
        )

      case _ => throw new InternalException("Expected a varlength path to be here")
    }
  }

  /**
   * `extractPredicates` extracts the Predicates ouf of the FilterScopes they are inside. The ListSubqueryExpressionSolver needs
   * to know if things are inside a different scope to work correctly. Otherwise it will plan RollupApply when not allowed,
   * or plan the wrong `NestedPlanExpression`. Since extracting the scope instead of the inner predicate is not straightforward,
   * the easiest solution is this one: we wrap each predicate in a FilterScope, give it to the ListSubqueryExpressionSolver,
   * and then extract it from the FilterScope again.
   */
  private def solveSubqueryExpressionsForExtractedPredicates(
    source: LogicalPlan,
    nodePredicates: Set[VariablePredicate],
    relationshipPredicates: Set[VariablePredicate],
    context: LogicalPlanningContext
  ): (Set[VariablePredicate], Set[VariablePredicate], LogicalPlan) = {
    val solver = SubqueryExpressionSolver.solverFor(source, context)

    def solveVariablePredicate(variablePredicate: VariablePredicate): VariablePredicate = {
      val filterScope = FilterScope(variablePredicate.variable, Some(variablePredicate.predicate))(
        variablePredicate.predicate.position
      )
      val rewrittenFilterScope = solver.solve(filterScope).asInstanceOf[FilterScope]
      VariablePredicate(rewrittenFilterScope.variable, rewrittenFilterScope.innerPredicate.get)
    }

    val rewrittenRelationshipPredicates = relationshipPredicates.map(solveVariablePredicate)
    val rewrittenNodePredicates = nodePredicates.map(solveVariablePredicate)
    val rewrittenSource = solver.rewrittenPlan()
    (rewrittenRelationshipPredicates, rewrittenNodePredicates, rewrittenSource)
  }

  def fixupTrailRhsPlan(
    originalPlan: LogicalPlan,
    argumentToRemove: String,
    predicatesToRemove: Set[Expression]
  ): LogicalPlan = {
    val fixedSolved = solveds.get(originalPlan.id).asSinglePlannerQuery.amendQueryGraph {
      qg =>
        // We added these in `PrecomputedQPPInnerPlans`, so for solved we have to remove it again.
        qg.removeArgumentId(argumentToRemove)
          .withSelections(qg.selections.filter(p => !predicatesToRemove.contains(p.expr)))
    }

    val newPlan = originalPlan.copyPlanWithIdGen(attributesWithoutSolveds.copy(originalPlan.id))
    solveds.set(newPlan.id, fixedSolved)
    newPlan
  }

  def planTrail(
    source: LogicalPlan,
    pattern: QuantifiedPathPattern,
    startBinding: NodeBinding,
    endBinding: NodeBinding,
    maybeHiddenFilter: Option[Expression],
    context: LogicalPlanningContext,
    innerPlan: LogicalPlan,
    predicates: Seq[Expression],
    previouslyBoundRelationships: Set[String],
    previouslyBoundRelationshipGroups: Set[String],
    reverseGroupVariableProjections: Boolean
  ): LogicalPlan = {
    // Ensure that innerPlan does conform with the pattern contained inside of the quantified path pattern before we mark it as solved
    try {
      VerifyBestPlan(innerPlan, SinglePlannerQuery.empty.withQueryGraph(pattern.asQueryGraph), context)
    } catch {
      case planVerificationException: InternalException => throw new InternalException(
          "The provided inner plan doesn't conform with the quantified path pattern being planned",
          planVerificationException
        )
    }

    val solved = solveds.get(source.id).asSinglePlannerQuery.amendQueryGraph(_
      .addQuantifiedPathPattern(pattern)
      .addPredicates(predicates: _*))

    val providedOrder = providedOrders.get(source.id).fromLeft
    val trailPlan = annotate(
      Trail(
        left = source,
        right = innerPlan,
        repetition = pattern.repetition,
        start = startBinding.outer,
        end = endBinding.outer,
        innerStart = startBinding.inner,
        innerEnd = endBinding.inner,
        nodeVariableGroupings = pattern.nodeVariableGroupings.map { case VariableGrouping(singleton, group) =>
          plans.Trail.VariableGrouping(singleton, group)
        },
        relationshipVariableGroupings = pattern.relationshipVariableGroupings.map {
          case VariableGrouping(singleton, group) =>
            plans.Trail.VariableGrouping(singleton, group)
        },
        innerRelationships = pattern.patternRelationships.map(_.name).toSet,
        previouslyBoundRelationships = previouslyBoundRelationships,
        previouslyBoundRelationshipGroups = previouslyBoundRelationshipGroups,
        reverseGroupVariableProjections = reverseGroupVariableProjections
      ),
      solved,
      providedOrder,
      context
    )

    maybeHiddenFilter match {
      case Some(hiddenFilter) =>
        annotateSelection(
          Selection(Seq(hiddenFilter), trailPlan),
          solved,
          providedOrder,
          context
        )
      case None => trailPlan
    }
  }

  def planNodeByIdSeek(
    variable: Variable,
    nodeIds: SeekableArgs,
    solvedPredicates: Seq[Expression] = Seq.empty,
    argumentIds: Set[String],
    context: LogicalPlanningContext
  ): LogicalPlan = {
    doPlanNodeByIdSeek(NodeByIdSeek.apply, variable, nodeIds, solvedPredicates, argumentIds, context)
  }

  def planNodeByElementIdSeek(
    variable: Variable,
    nodeIds: SeekableArgs,
    solvedPredicates: Seq[Expression] = Seq.empty,
    argumentIds: Set[String],
    context: LogicalPlanningContext
  ): LogicalPlan = {
    doPlanNodeByIdSeek(NodeByElementIdSeek.apply, variable, nodeIds, solvedPredicates, argumentIds, context)
  }

  private def doPlanNodeByIdSeek(
    makePlan: (String, SeekableArgs, Set[String]) => NodeLogicalLeafPlan,
    variable: Variable,
    nodeIds: SeekableArgs,
    solvedPredicates: Seq[Expression],
    argumentIds: Set[String],
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = RegularSinglePlannerQuery(queryGraph =
      QueryGraph.empty
        .addPatternNodes(variable.name)
        .addPredicates(solvedPredicates: _*)
        .addArgumentIds(argumentIds.toIndexedSeq)
    )
    val solver = SubqueryExpressionSolver.solverForLeafPlan(argumentIds, context)
    val rewrittenNodeIds = nodeIds.mapValues(solver.solve(_))
    val newArguments = solver.newArguments
    val leafPlan = annotate(
      makePlan(variable.name, rewrittenNodeIds, argumentIds ++ newArguments),
      solved,
      ProvidedOrder.empty,
      context
    )
    solver.rewriteLeafPlan(leafPlan)
  }

  def planNodeByLabelScan(
    variable: Variable,
    label: LabelName,
    solvedPredicates: Seq[Expression],
    solvedHint: Option[UsingScanHint] = None,
    argumentIds: Set[String],
    providedOrder: ProvidedOrder,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = RegularSinglePlannerQuery(queryGraph =
      QueryGraph.empty
        .addPatternNodes(variable.name)
        .addPredicates(solvedPredicates: _*)
        .addHints(solvedHint)
        .addArgumentIds(argumentIds.toIndexedSeq)
    )
    annotate(
      NodeByLabelScan(variable.name, label, argumentIds, toIndexOrder(providedOrder)),
      solved,
      providedOrder,
      context
    )
  }

  def planUnionNodeByLabelsScan(
    variable: Variable,
    labels: Seq[LabelName],
    solvedPredicates: Seq[Expression],
    solvedHints: Seq[UsingScanHint] = Seq.empty,
    argumentIds: Set[String],
    providedOrder: ProvidedOrder,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = RegularSinglePlannerQuery(queryGraph =
      QueryGraph.empty
        .addPatternNodes(variable.name)
        .addPredicates(solvedPredicates: _*)
        .addHints(solvedHints)
        .addArgumentIds(argumentIds.toIndexedSeq)
    )
    annotate(
      UnionNodeByLabelsScan(variable.name, labels, argumentIds, toIndexOrder(providedOrder)),
      solved,
      providedOrder,
      context
    )
  }

  def planIntersectNodeByLabelsScan(
    variable: Variable,
    labels: Seq[LabelName],
    solvedPredicates: Seq[Expression],
    solvedHints: Seq[UsingScanHint] = Seq.empty,
    argumentIds: Set[String],
    providedOrder: ProvidedOrder,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = RegularSinglePlannerQuery(queryGraph =
      QueryGraph.empty
        .addPatternNodes(variable.name)
        .addPredicates(solvedPredicates: _*)
        .addHints(solvedHints)
        .addArgumentIds(argumentIds.toIndexedSeq)
    )
    annotate(
      IntersectionNodeByLabelsScan(variable.name, labels, argumentIds, toIndexOrder(providedOrder)),
      solved,
      providedOrder,
      context
    )
  }

  def planNodeIndexSeek(
    idName: String,
    label: LabelToken,
    properties: Seq[IndexedProperty],
    valueExpr: QueryExpression[Expression],
    solvedPredicates: Seq[Expression] = Seq.empty,
    solvedHint: Option[UsingIndexHint] = None,
    argumentIds: Set[String],
    providedOrder: ProvidedOrder,
    indexOrder: IndexOrder,
    context: LogicalPlanningContext,
    indexType: IndexType
  ): LogicalPlan = {
    val queryGraph = QueryGraph.empty
      .addPatternNodes(idName)
      .addPredicates(solvedPredicates: _*)
      .addHints(solvedHint)
      .addArgumentIds(argumentIds.toIndexedSeq)

    val solved = RegularSinglePlannerQuery(queryGraph = queryGraph)

    val solver = SubqueryExpressionSolver.solverForLeafPlan(argumentIds, context)
    val rewrittenValueExpr = valueExpr.map(solver.solve(_))
    val newArguments = solver.newArguments

    val plan = NodeIndexSeek(
      idName,
      label,
      properties,
      rewrittenValueExpr,
      argumentIds ++ newArguments,
      indexOrder,
      indexType.toPublicApi
    )
    val annotatedPlan = annotate(plan, solved, providedOrder, context)

    solver.rewriteLeafPlan(annotatedPlan)
  }

  def planNodeIndexScan(
    idName: String,
    label: LabelToken,
    properties: Seq[IndexedProperty],
    solvedPredicates: Seq[Expression] = Seq.empty,
    solvedHint: Option[UsingIndexHint] = None,
    argumentIds: Set[String],
    providedOrder: ProvidedOrder,
    indexOrder: IndexOrder,
    context: LogicalPlanningContext,
    indexType: IndexType
  ): LogicalPlan = {
    val solved = RegularSinglePlannerQuery(queryGraph =
      QueryGraph.empty
        .addPatternNodes(idName)
        .addPredicates(solvedPredicates: _*)
        .addHints(solvedHint)
        .addArgumentIds(argumentIds.toIndexedSeq)
    )
    annotate(
      NodeIndexScan(idName, label, properties, argumentIds, indexOrder, indexType.toPublicApi),
      solved,
      providedOrder,
      context
    )
  }

  def planNodeIndexStringSearchScan(
    idName: String,
    label: LabelToken,
    properties: Seq[IndexedProperty],
    stringSearchMode: StringSearchMode,
    solvedPredicates: Seq[Expression],
    solvedHint: Option[UsingIndexHint],
    valueExpr: Expression,
    argumentIds: Set[String],
    providedOrder: ProvidedOrder,
    indexOrder: IndexOrder,
    context: LogicalPlanningContext,
    indexType: IndexType
  ): LogicalPlan = {
    val solved = RegularSinglePlannerQuery(queryGraph =
      QueryGraph.empty
        .addPatternNodes(idName)
        .addPredicates(solvedPredicates: _*)
        .addHints(solvedHint)
        .addArgumentIds(argumentIds.toIndexedSeq)
    )
    val solver = SubqueryExpressionSolver.solverForLeafPlan(argumentIds, context)
    val rewrittenValueExpr = solver.solve(valueExpr)
    val newArguments = solver.newArguments

    val planTemplate = stringSearchMode match {
      case ContainsSearchMode => NodeIndexContainsScan(_, _, _, _, _, _, _)
      case EndsWithSearchMode => NodeIndexEndsWithScan(_, _, _, _, _, _, _)
    }

    val plan = planTemplate(
      idName,
      label,
      properties.head,
      rewrittenValueExpr,
      argumentIds ++ newArguments,
      indexOrder,
      indexType.toPublicApi
    )
    val annotatedPlan = annotate(plan, solved, providedOrder, context)

    solver.rewriteLeafPlan(annotatedPlan)
  }

  def planNodeHashJoin(
    nodes: Set[String],
    left: LogicalPlan,
    right: LogicalPlan,
    hints: Set[UsingJoinHint],
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val plannerQuery = solveds.get(left.id).asSinglePlannerQuery ++ solveds.get(right.id).asSinglePlannerQuery
    val solved = plannerQuery.amendQueryGraph(_.addHints(hints))
    annotate(NodeHashJoin(nodes, left, right), solved, providedOrders.get(right.id).fromRight, context)
  }

  def planValueHashJoin(
    left: LogicalPlan,
    right: LogicalPlan,
    join: Equals,
    originalPredicate: Expression,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val plannerQuery = solveds.get(left.id).asSinglePlannerQuery ++ solveds.get(right.id).asSinglePlannerQuery
    val solved = plannerQuery.amendQueryGraph(_.addPredicates(originalPredicate))
    // `join` is an Expression that could go through the ListSubqueryExpressionSolver, but a value hash join
    // is only planned for Expressions such as `lhs.prop = rhs.prop`
    annotate(ValueHashJoin(left, right, join), solved, providedOrders.get(right.id).fromRight, context)
  }

  def planNodeUniqueIndexSeek(
    idName: String,
    label: LabelToken,
    properties: Seq[IndexedProperty],
    valueExpr: QueryExpression[Expression],
    solvedPredicates: Seq[Expression] = Seq.empty,
    solvedHint: Option[UsingIndexHint] = None,
    argumentIds: Set[String],
    providedOrder: ProvidedOrder,
    indexOrder: IndexOrder,
    context: LogicalPlanningContext,
    indexType: IndexType
  ): LogicalPlan = {
    val queryGraph = QueryGraph.empty
      .addPatternNodes(idName)
      .addPredicates(solvedPredicates: _*)
      .addHints(solvedHint)
      .addArgumentIds(argumentIds.toIndexedSeq)

    val solved = RegularSinglePlannerQuery(queryGraph = queryGraph)

    val solver = SubqueryExpressionSolver.solverForLeafPlan(argumentIds, context)
    val rewrittenValueExpr = valueExpr.map(solver.solve(_))
    val newArguments = solver.newArguments

    val plan = NodeUniqueIndexSeek(
      idName,
      label,
      properties,
      rewrittenValueExpr,
      argumentIds ++ newArguments,
      indexOrder,
      indexType.toPublicApi
    )
    val annotatedPlan = annotate(plan, solved, providedOrder, context)

    solver.rewriteLeafPlan(annotatedPlan)

  }

  def planAssertSameNode(
    node: String,
    left: LogicalPlan,
    right: LogicalPlan,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved: SinglePlannerQuery =
      solveds.get(left.id).asSinglePlannerQuery ++ solveds.get(right.id).asSinglePlannerQuery
    annotate(AssertSameNode(node, left, right), solved, providedOrders.get(left.id).fromLeft, context)
  }

  def planAssertSameRelationship(
    relationship: PatternRelationship,
    left: LogicalPlan,
    right: LogicalPlan,
    context: LogicalPlanningContext
  ): LogicalPlan =
    annotate(
      AssertSameRelationship(relationship.name, left, right),
      solveds.get(left.id).asSinglePlannerQuery ++ solveds.get(right.id).asSinglePlannerQuery,
      providedOrders.get(left.id).fromLeft,
      context
    )

  def planOptional(
    inputPlan: LogicalPlan,
    ids: Set[String],
    context: LogicalPlanningContext,
    optionalQG: QueryGraph
  ): LogicalPlan = {
    val patternNodes =
      optionalQG
        .patternNodes
        .intersect(ids)
        .toSeq

    val patternRelationships =
      optionalQG
        .patternRelationships
        .filter(rel => ids(rel.name))

    val optionalMatchQG =
      solveds
        .get(inputPlan.id)
        .asSinglePlannerQuery
        .queryGraph
        .addPatternNodes(patternNodes: _*)
        .addPatternRelationships(patternRelationships)

    val solved = RegularSinglePlannerQuery(queryGraph =
      QueryGraph.empty
        .withAddedOptionalMatch(optionalMatchQG)
        .withArgumentIds(ids)
    )

    annotate(Optional(inputPlan, ids), solved, providedOrders.get(inputPlan.id).fromLeft, context)
  }

  def planLeftOuterHashJoin(
    nodes: Set[String],
    left: LogicalPlan,
    right: LogicalPlan,
    hints: Set[UsingJoinHint],
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(left.id).asSinglePlannerQuery.amendQueryGraph(
      _.withAddedOptionalMatch(solveds.get(right.id).asSinglePlannerQuery.queryGraph.addHints(hints))
    )
    val inputOrder = providedOrders.get(right.id)
    val providedOrder =
      if (inputOrder.columns.exists(!_.isAscending)) {
        // Join nodes that are not matched from the RHS will result in rows with null in the Sort column.
        // These nulls will always be at the end. That is the correct order for ASC.
        // If there is at least a DESC column, we cannot provide any order.
        ProvidedOrder.empty
      } else {
        // If the order is on a join column (or derived from a join column), we cannot continue guaranteeing that order.
        // The join nodes that are not matched from the RHS will appear out-of-order after all join nodes which were matched.
        inputOrder.upToExcluding(nodes).fromRight
      }
    annotate(LeftOuterHashJoin(nodes, left, right), solved, providedOrder, context)
  }

  def planRightOuterHashJoin(
    nodes: Set[String],
    left: LogicalPlan,
    right: LogicalPlan,
    hints: Set[UsingJoinHint],
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(right.id).asSinglePlannerQuery.amendQueryGraph(
      _.withAddedOptionalMatch(solveds.get(left.id).asSinglePlannerQuery.queryGraph.addHints(hints))
    )
    annotate(RightOuterHashJoin(nodes, left, right), solved, providedOrders.get(right.id).fromRight, context)
  }

  def planSelection(source: LogicalPlan, predicates: Seq[Expression], context: LogicalPlanningContext): LogicalPlan = {
    val solved =
      solveds.get(source.id).asSinglePlannerQuery.updateTailOrSelf(_.amendQueryGraph(_.addPredicates(predicates: _*)))
    val (rewrittenPredicates, rewrittenSource) =
      SubqueryExpressionSolver.ForMulti.solve(source, predicates, context)
    annotateSelection(
      Selection(coercePredicatesWithAnds(rewrittenPredicates), rewrittenSource),
      solved,
      providedOrders.get(source.id).fromLeft,
      context
    )
  }

  def planHorizonSelection(
    source: LogicalPlan,
    predicates: Seq[Expression],
    interestingOrderConfig: InterestingOrderConfig,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(source.id).asSinglePlannerQuery.updateTailOrSelf(_.updateHorizon {
      case p: QueryProjection => p.addPredicates(predicates: _*)
      case _ => throw new IllegalArgumentException("You can only plan HorizonSelection after a projection")
    })

    // solve existential subquery predicates
    val (solvedPredicates, existsPlan) =
      SubqueryExpressionSolver.ForExistentialSubquery.solve(source, predicates, interestingOrderConfig, context)
    val unsolvedPredicates = predicates.filterNot(solvedPredicates.contains(_))

    // solve remaining predicates
    val newPlan =
      if (unsolvedPredicates.nonEmpty) {
        val (rewrittenPredicates, rewrittenSource) =
          SubqueryExpressionSolver.ForMulti.solve(existsPlan, unsolvedPredicates, context)
        annotateSelection(
          Selection(coercePredicatesWithAnds(rewrittenPredicates), rewrittenSource),
          solved,
          providedOrders.get(existsPlan.id).fromLeft,
          context
        )
      } else {
        existsPlan
      }

    newPlan
  }

  /**
   * Plan a selection with `solved` already given.
   * The predicates are not run through the [[SubqueryExpressionSolver]], so they must not contain any IR expressions.
   */
  private def planSelectionWithGivenSolved(
    source: LogicalPlan,
    predicates: Seq[Expression],
    solved: PlannerQuery,
    context: LogicalPlanningContext
  ): Selection = {
    annotateSelection(
      Selection(coercePredicatesWithAnds(predicates), source),
      solved,
      providedOrders.get(source.id).fromLeft,
      context
    )
  }

  // Using the solver for `expr` in all SemiApply-like plans is kinda stupid.
  // The idea is that `expr` is cheap to evaluate while the subquery (`inner`) is costly.
  // If `expr` is _also_ an IRExpression, that is not true any longer,
  // and it could be cheaper to execute the one subquery  (`inner`) instead of the other (`expr`).

  def planSelectOrAntiSemiApply(
    outer: LogicalPlan,
    inner: LogicalPlan,
    expr: Expression,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val (rewrittenExpr, rewrittenOuter) = SubqueryExpressionSolver.ForSingle.solve(outer, expr, context)
    annotate(
      SelectOrAntiSemiApply(rewrittenOuter, inner, rewrittenExpr),
      solveds.get(outer.id),
      providedOrders.get(outer.id).fromLeft,
      context
    )
  }

  def planLetSelectOrAntiSemiApply(
    outer: LogicalPlan,
    inner: LogicalPlan,
    id: String,
    expr: Expression,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val (rewrittenExpr, rewrittenOuter) = SubqueryExpressionSolver.ForSingle.solve(outer, expr, context)
    annotate(
      LetSelectOrAntiSemiApply(rewrittenOuter, inner, id, rewrittenExpr),
      solveds.get(outer.id),
      providedOrders.get(outer.id).fromLeft,
      context
    )
  }

  def planSelectOrSemiApply(
    outer: LogicalPlan,
    inner: LogicalPlan,
    expr: Expression,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val (rewrittenExpr, rewrittenOuter) = SubqueryExpressionSolver.ForSingle.solve(outer, expr, context)
    annotate(
      SelectOrSemiApply(rewrittenOuter, inner, rewrittenExpr),
      solveds.get(outer.id),
      providedOrders.get(outer.id).fromLeft,
      context
    )
  }

  def planLetSelectOrSemiApply(
    outer: LogicalPlan,
    inner: LogicalPlan,
    id: String,
    expr: Expression,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val (rewrittenExpr, rewrittenOuter) = SubqueryExpressionSolver.ForSingle.solve(outer, expr, context)
    annotate(
      LetSelectOrSemiApply(rewrittenOuter, inner, id, rewrittenExpr),
      solveds.get(outer.id),
      providedOrders.get(outer.id).fromLeft,
      context
    )
  }

  def planLetAntiSemiApply(
    left: LogicalPlan,
    right: LogicalPlan,
    id: String,
    context: LogicalPlanningContext
  ): LogicalPlan =
    annotate(LetAntiSemiApply(left, right, id), solveds.get(left.id), providedOrders.get(left.id).fromLeft, context)

  def planLetSemiApply(
    left: LogicalPlan,
    right: LogicalPlan,
    id: String,
    context: LogicalPlanningContext
  ): LogicalPlan =
    annotate(LetSemiApply(left, right, id), solveds.get(left.id), providedOrders.get(left.id).fromLeft, context)

  def planAntiSemiApply(
    left: LogicalPlan,
    right: LogicalPlan,
    expr: Expression,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(left.id).asSinglePlannerQuery.updateTailOrSelf(_.amendQueryGraph(_.addPredicates(expr)))
    annotate(AntiSemiApply(left, right), solved, providedOrders.get(left.id).fromLeft, context)
  }

  def planSemiApply(
    left: LogicalPlan,
    right: LogicalPlan,
    expr: Expression,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(left.id).asSinglePlannerQuery.updateTailOrSelf(_.amendQueryGraph(_.addPredicates(expr)))
    annotate(SemiApply(left, right), solved, providedOrders.get(left.id).fromLeft, context)
  }

  def planSemiApplyInHorizon(
    left: LogicalPlan,
    right: LogicalPlan,
    expr: Expression,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(left.id).asSinglePlannerQuery.updateTailOrSelf(_.updateHorizon {
      case horizon: QueryProjection => horizon.addPredicates(expr)
      case horizon                  => horizon
    })
    annotate(SemiApply(left, right), solved, providedOrders.get(left.id).fromLeft, context)
  }

  def planAntiSemiApplyInHorizon(
    left: LogicalPlan,
    right: LogicalPlan,
    expr: Expression,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(left.id).asSinglePlannerQuery.updateTailOrSelf(_.updateHorizon {
      case horizon: QueryProjection => horizon.addPredicates(expr)
      case horizon                  => horizon
    })
    annotate(AntiSemiApply(left, right), solved, providedOrders.get(left.id).fromLeft, context)
  }

  def planQueryArgument(queryGraph: QueryGraph, context: LogicalPlanningContext): LogicalPlan = {
    val patternNodes = queryGraph.argumentIds intersect queryGraph.patternNodes
    val patternRels =
      queryGraph.patternRelationships.filter(rel => queryGraph.argumentIds.contains(rel.name)).map(_.name)
    val otherIds = queryGraph.argumentIds -- patternNodes
    planArgument(patternNodes, patternRels, otherIds, context)
  }

  def planArgument(
    patternNodes: Set[String],
    patternRels: Set[String] = Set.empty,
    other: Set[String] = Set.empty,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val coveredIds = patternNodes ++ patternRels ++ other

    val solved = RegularSinglePlannerQuery(queryGraph =
      QueryGraph(
        argumentIds = coveredIds,
        patternNodes = patternNodes,
        patternRelationships = Set.empty
      )
    )

    annotate(Argument(coveredIds), solved, ProvidedOrder.empty, context)
  }

  def planArgument(context: LogicalPlanningContext): LogicalPlan =
    annotate(Argument(Set.empty), SinglePlannerQuery.empty, ProvidedOrder.empty, context)

  def planEmptyProjection(inner: LogicalPlan, context: LogicalPlanningContext): LogicalPlan =
    annotate(EmptyResult(inner), solveds.get(inner.id), ProvidedOrder.empty, context)

  def planStarProjection(inner: LogicalPlan, reported: Option[Map[String, Expression]]): LogicalPlan = {
    reported.fold(inner) { reported =>
      val newSolved: SinglePlannerQuery = solveds.get(inner.id).asSinglePlannerQuery.updateTailOrSelf(
        _.updateQueryProjection(_.withAddedProjections(reported))
      )
      // Keep some attributes, but change solved
      val keptAttributes = Attributes(idGen, cardinalities, providedOrders, leveragedOrders, labelAndRelTypeInfos)
      val newPlan = inner.copyPlanWithIdGen(keptAttributes.copy(inner.id))
      solveds.set(newPlan.id, newSolved)
      newPlan
    }
  }

  /**
   * @param expressions must be solved by the ListSubqueryExpressionSolver. This is not done here since that can influence the projection list,
   *                    thus this logic is put into [[projection]] instead.
   */
  def planRegularProjection(
    inner: LogicalPlan,
    discardSymbols: Set[String],
    expressions: Map[String, Expression],
    reported: Option[Map[String, Expression]],
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val innerSolved: SinglePlannerQuery = solveds.get(inner.id).asSinglePlannerQuery
    val solved = reported.fold(innerSolved) { reported =>
      innerSolved.updateTailOrSelf(_.updateQueryProjection(_.withAddedProjections(reported)))
    }
    planRegularProjectionHelper(inner, discardSymbols, expressions, context, solved)
  }

  /**
   * @param grouping                 must be solved by the ListSubqueryExpressionSolver. This is not done here since that can influence if we plan aggregation or projection, etc,
   *                                 thus this logic is put into [[aggregation]] instead.
   * @param aggregation              must be solved by the ListSubqueryExpressionSolver.
   * @param previousInterestingOrder the interesting order of the previous query part, if there was a previous part
   */
  def planAggregation(
    left: LogicalPlan,
    grouping: Map[String, Expression],
    aggregation: Map[String, Expression],
    reportedGrouping: Map[String, Expression],
    reportedAggregation: Map[String, Expression],
    previousInterestingOrder: Option[InterestingOrder],
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(left.id).asSinglePlannerQuery.updateTailOrSelf(_.withHorizon(
      AggregatingQueryProjection(groupingExpressions = reportedGrouping, aggregationExpressions = reportedAggregation)
    ))

    val trimmedAndRenamed = trimAndRenameProvidedOrder(providedOrders.get(left.id), grouping)

    val plan = annotate(
      Aggregation(left, grouping, aggregation),
      solved,
      context.providedOrderFactory.providedOrder(trimmedAndRenamed, ProvidedOrder.Left),
      context
    )

    def hasCollectOrUDF = aggregation.values.exists {
      case fi: FunctionInvocation => fi.function == Collect || fi.function == UnresolvedFunction
      case _                      => false
    }
    // Aggregation functions may leverage the order of a preceding ORDER BY.
    // In practice, this is only collect and potentially user defined aggregations
    if (previousInterestingOrder.exists(_.requiredOrderCandidate.nonEmpty) && hasCollectOrUDF) {
      markOrderAsLeveragedBackwardsUntilOrigin(plan, context.providedOrderFactory)
    }

    plan
  }

  def planOrderedAggregation(
    left: LogicalPlan,
    grouping: Map[String, Expression],
    aggregation: Map[String, Expression],
    orderToLeverage: Seq[Expression],
    reportedGrouping: Map[String, Expression],
    reportedAggregation: Map[String, Expression],
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(left.id).asSinglePlannerQuery.updateTailOrSelf(_.withHorizon(
      AggregatingQueryProjection(groupingExpressions = reportedGrouping, aggregationExpressions = reportedAggregation)
    ))

    val trimmedAndRenamed = trimAndRenameProvidedOrder(providedOrders.get(left.id), grouping)

    val plan = annotate(
      OrderedAggregation(left, grouping, aggregation, orderToLeverage),
      solved,
      context.providedOrderFactory.providedOrder(trimmedAndRenamed, ProvidedOrder.Left),
      context
    )
    markOrderAsLeveragedBackwardsUntilOrigin(plan, context.providedOrderFactory)
    plan
  }

  /**
   * The only purpose of this method is to set the solved correctly for something that is already sorted.
   */
  def updateSolvedForSortedItems(
    inner: LogicalPlan,
    interestingOrder: InterestingOrder,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    // Keep some attributes, but change solved
    val keptAttributes = Attributes(idGen, cardinalities, leveragedOrders, labelAndRelTypeInfos)
    val newPlan = inner.copyPlanWithIdGen(keptAttributes.copy(inner.id))
    val solved = solveds.get(inner.id).asSinglePlannerQuery.updateTailOrSelf(_.withInterestingOrder(interestingOrder))
    val providedOrder = providedOrders.get(inner.id)
    annotate(newPlan, solved, providedOrder, context)
  }

  def planCountStoreNodeAggregation(
    query: SinglePlannerQuery,
    projectedColumn: String,
    labels: List[Option[LabelName]],
    argumentIds: Set[String],
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = RegularSinglePlannerQuery(query.queryGraph, query.interestingOrder, query.horizon)
    annotate(
      NodeCountFromCountStore(projectedColumn, labels, argumentIds),
      solved,
      query.interestingOrder.requiredOrderCandidate.asProvidedOrder(context.providedOrderFactory),
      context
    )
  }

  def planCountStoreRelationshipAggregation(
    query: SinglePlannerQuery,
    idName: String,
    startLabel: Option[LabelName],
    typeNames: Seq[RelTypeName],
    endLabel: Option[LabelName],
    argumentIds: Set[String],
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved: SinglePlannerQuery = RegularSinglePlannerQuery(query.queryGraph, query.interestingOrder, query.horizon)
    annotate(
      RelationshipCountFromCountStore(idName, startLabel, typeNames, endLabel, argumentIds),
      solved,
      query.interestingOrder.requiredOrderCandidate.asProvidedOrder(context.providedOrderFactory),
      context
    )
  }

  def planSkip(
    inner: LogicalPlan,
    count: Expression,
    interestingOrder: InterestingOrder,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    // `count` is not allowed to be an IRExpression
    val solved = solveds.get(inner.id).asSinglePlannerQuery.updateTailOrSelf(
      _.updateQueryProjection(_.updatePagination(_.withSkipExpression(count)))
    )
    val plan = annotate(Skip(inner, count), solved, providedOrders.get(inner.id).fromLeft, context)
    if (interestingOrder.requiredOrderCandidate.nonEmpty) {
      markOrderAsLeveragedBackwardsUntilOrigin(plan, context.providedOrderFactory)
    }
    plan
  }

  def planLoadCSV(
    inner: LogicalPlan,
    variableName: String,
    url: Expression,
    format: CSVFormat,
    fieldTerminator: Option[StringLiteral],
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.updateTailOrSelf(_.withHorizon(LoadCSVProjection(
      variableName,
      url,
      format,
      fieldTerminator
    )))
    val (rewrittenUrl, rewrittenInner) = SubqueryExpressionSolver.ForSingle.solve(inner, url, context)
    annotate(
      LoadCSV(
        rewrittenInner,
        rewrittenUrl,
        variableName,
        format,
        fieldTerminator.map(_.value),
        context.settings.legacyCsvQuoteEscaping,
        context.settings.csvBufferSize
      ),
      solved,
      providedOrders.get(rewrittenInner.id).fromLeft,
      context
    )
  }

  def planInput(symbols: Seq[Variable], context: LogicalPlanningContext): LogicalPlan = {
    val solved = RegularSinglePlannerQuery(queryInput = Some(symbols))
    annotate(Input(symbols.map(_.name)), solved, ProvidedOrder.empty, context)
  }

  def planUnwind(
    inner: LogicalPlan,
    name: String,
    expression: Expression,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved =
      solveds.get(inner.id).asSinglePlannerQuery.updateTailOrSelf(_.withHorizon(UnwindProjection(name, expression)))
    val (rewrittenExpression, rewrittenInner) = SubqueryExpressionSolver.ForSingle.solve(inner, expression, context)
    annotate(
      UnwindCollection(rewrittenInner, name, rewrittenExpression),
      solved,
      providedOrders.get(rewrittenInner.id).fromLeft,
      context
    )
  }

  def planProcedureCall(inner: LogicalPlan, call: ResolvedCall, context: LogicalPlanningContext): LogicalPlan = {
    val solved =
      solveds.get(inner.id).asSinglePlannerQuery.updateTailOrSelf(_.withHorizon(ProcedureCallProjection(call)))
    val solver = SubqueryExpressionSolver.solverFor(inner, context)
    val rewrittenCall = call.mapCallArguments(solver.solve(_))
    val rewrittenInner = solver.rewrittenPlan()
    val providedOrder =
      if (call.containsNoUpdates) providedOrders.get(rewrittenInner.id).fromLeft else ProvidedOrder.empty
    annotate(ProcedureCall(rewrittenInner, rewrittenCall), solved, providedOrder, context)
  }

  def planCommand(inner: LogicalPlan, clause: CommandClause, context: LogicalPlanningContext): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.updateTailOrSelf(_.withHorizon(CommandProjection(clause)))

    val plan = clause match {
      case s: ShowIndexesClause =>
        ShowIndexes(s.indexType, s.unfilteredColumns.useAllColumns, s.unfilteredColumns.columns)
      case s: ShowConstraintsClause =>
        ShowConstraints(s.constraintType, s.unfilteredColumns.useAllColumns, s.unfilteredColumns.columns)
      case s: ShowProceduresClause =>
        ShowProcedures(s.executable, s.unfilteredColumns.useAllColumns, s.unfilteredColumns.columns)
      case s: ShowFunctionsClause =>
        ShowFunctions(s.functionType, s.executable, s.unfilteredColumns.useAllColumns, s.unfilteredColumns.columns)
      case s: ShowTransactionsClause =>
        ShowTransactions(
          s.ids,
          s.unfilteredColumns.useAllColumns,
          s.unfilteredColumns.columns,
          s.yieldItems,
          s.yieldAll
        )
      case s: TerminateTransactionsClause =>
        TerminateTransactions(s.ids, s.unfilteredColumns.columns, s.yieldItems, s.yieldAll)
      case s: ShowSettingsClause =>
        ShowSettings(s.names, s.unfilteredColumns.useAllColumns, s.unfilteredColumns.columns)
    }
    val annotatedPlan = annotate(plan, solved, ProvidedOrder.empty, context)

    val apply = Apply(inner, annotatedPlan)
    annotate(apply, solved, ProvidedOrder.empty, context)
  }

  def planPassAll(inner: LogicalPlan, context: LogicalPlanningContext): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.updateTailOrSelf(_.withHorizon(PassthroughAllHorizon()))
    // Keep some attributes, but change solved
    val keptAttributes = Attributes(idGen, cardinalities, leveragedOrders, labelAndRelTypeInfos)
    val newPlan = inner.copyPlanWithIdGen(keptAttributes.copy(inner.id))
    annotate(newPlan, solved, providedOrders.get(inner.id).fromLeft, context)
  }

  def planLimit(
    inner: LogicalPlan,
    effectiveCount: Expression,
    reportedCount: Expression,
    interestingOrder: InterestingOrder,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    // `effectiveCount` is not allowed to be an IRExpression
    val solved = solveds.get(inner.id).asSinglePlannerQuery.updateTailOrSelf(
      _.updateQueryProjection(_.updatePagination(_.withLimitExpression(reportedCount)))
    )
    val plan = annotate(Limit(inner, effectiveCount), solved, providedOrders.get(inner.id).fromLeft, context)
    if (interestingOrder.requiredOrderCandidate.nonEmpty) {
      markOrderAsLeveragedBackwardsUntilOrigin(plan, context.providedOrderFactory)
    }
    plan
  }

  def planExhaustiveLimit(
    inner: LogicalPlan,
    effectiveCount: Expression,
    reportedCount: Expression,
    interestingOrder: InterestingOrder,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    // `effectiveCount` is not allowed to be an IRExpression
    val solved = solveds.get(inner.id).asSinglePlannerQuery.updateTailOrSelf(
      _.updateQueryProjection(_.updatePagination(_.withLimitExpression(reportedCount)))
    )
    val plan = annotate(ExhaustiveLimit(inner, effectiveCount), solved, providedOrders.get(inner.id).fromLeft, context)
    if (interestingOrder.requiredOrderCandidate.nonEmpty) {
      markOrderAsLeveragedBackwardsUntilOrigin(plan, context.providedOrderFactory)
    }
    plan
  }

  // In case we have SKIP n LIMIT m, we want to limit by (n + m), since we plan the Limit before the Skip.
  def planSkipAndLimit(
    inner: LogicalPlan,
    skipExpr: Expression,
    limitExpr: Expression,
    interestingOrder: InterestingOrder,
    context: LogicalPlanningContext,
    useExhaustiveLimit: Boolean
  ): LogicalPlan = {
    val solvedSkip = solveds.get(inner.id).asSinglePlannerQuery.updateTailOrSelf(
      _.updateQueryProjection(_.updatePagination(_.withSkipExpression(skipExpr)))
    )
    val solvedSkipAndLimit =
      solvedSkip.updateTailOrSelf(_.updateQueryProjection(_.updatePagination(_.withLimitExpression(limitExpr))))

    val skipCardinality = cardinalityModel(
      solvedSkip,
      context.plannerState.input.labelInfo,
      context.plannerState.input.relTypeInfo,
      context.semanticTable,
      context.plannerState.indexCompatiblePredicatesProviderContext
    )
    val limitCardinality = cardinalityModel(
      solvedSkipAndLimit,
      context.plannerState.input.labelInfo,
      context.plannerState.input.relTypeInfo,
      context.semanticTable,
      context.plannerState.indexCompatiblePredicatesProviderContext
    )
    val innerCardinality = cardinalities.get(inner.id)
    val skippedRows = innerCardinality - skipCardinality

    val effectiveLimitExpr = Add(limitExpr, skipExpr)(limitExpr.position)
    val limitPlan =
      if (useExhaustiveLimit) {
        planExhaustiveLimit(inner, effectiveLimitExpr, limitExpr, interestingOrder, context)
      } else {
        planLimit(inner, effectiveLimitExpr, limitExpr, interestingOrder, context)
      }
    cardinalities.set(limitPlan.id, skippedRows + limitCardinality)

    planSkip(limitPlan, skipExpr, interestingOrder, context)
  }

  def planLimitForAggregation(
    inner: LogicalPlan,
    reportedGrouping: Map[String, Expression],
    reportedAggregation: Map[String, Expression],
    interestingOrder: InterestingOrder,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.updateTailOrSelf(_.withHorizon(
      AggregatingQueryProjection(groupingExpressions = reportedGrouping, aggregationExpressions = reportedAggregation)
    ).withInterestingOrder(interestingOrder))
    val providedOrder = providedOrders.get(inner.id).fromLeft
    val limitPlan = planLimitOnTopOf(inner, SignedDecimalIntegerLiteral("1")(InputPosition.NONE))
    val annotatedLimitPlan = annotate(limitPlan, solved, providedOrder, context)

    // The limit leverages the order, not the following optional
    markOrderAsLeveragedBackwardsUntilOrigin(annotatedLimitPlan, context.providedOrderFactory)

    val plan = Optional(annotatedLimitPlan)
    annotate(plan, solved, providedOrder, context)
  }

  def planSort(
    inner: LogicalPlan,
    sortColumns: Seq[ColumnOrder],
    orderColumns: Seq[ordering.ColumnOrder],
    interestingOrder: InterestingOrder,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.updateTailOrSelf(_.withInterestingOrder(interestingOrder))
    annotate(
      Sort(inner, sortColumns),
      solved,
      context.providedOrderFactory.providedOrder(orderColumns, ProvidedOrder.Self),
      context
    )
  }

  def planTop(
    inner: LogicalPlan,
    limit: Expression,
    sortColumns: Seq[ColumnOrder],
    orderColumns: Seq[ordering.ColumnOrder],
    interestingOrder: InterestingOrder,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.updateTailOrSelf(_.withInterestingOrder(interestingOrder)
      .updateQueryProjection(_.updatePagination(_.withLimitExpression(limit))))
    val top = annotate(
      Top(inner, sortColumns, limit),
      solved,
      context.providedOrderFactory.providedOrder(orderColumns, ProvidedOrder.Self),
      context
    )
    if (interestingOrder.requiredOrderCandidate.nonEmpty) {
      markOrderAsLeveragedBackwardsUntilOrigin(top, context.providedOrderFactory)
    }
    top
  }

  def planTop1WithTies(
    inner: LogicalPlan,
    sortColumns: Seq[ColumnOrder],
    orderColumns: Seq[ordering.ColumnOrder],
    interestingOrder: InterestingOrder,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.updateTailOrSelf(_.withInterestingOrder(interestingOrder)
      .updateQueryProjection(
        _.updatePagination(_.withLimitExpression(SignedDecimalIntegerLiteral("1")(InputPosition.NONE)))
      ))
    val top = annotate(
      Top1WithTies(inner, sortColumns),
      solved,
      context.providedOrderFactory.providedOrder(orderColumns, ProvidedOrder.Self),
      context
    )
    if (interestingOrder.requiredOrderCandidate.nonEmpty) {
      markOrderAsLeveragedBackwardsUntilOrigin(top, context.providedOrderFactory)
    }
    top
  }

  def planPartialSort(
    inner: LogicalPlan,
    alreadySortedPrefix: Seq[ColumnOrder],
    stillToSortSuffix: Seq[ColumnOrder],
    orderColumns: Seq[ordering.ColumnOrder],
    interestingOrder: InterestingOrder,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.updateTailOrSelf(_.withInterestingOrder(interestingOrder))
    val plan = annotate(
      PartialSort(inner, alreadySortedPrefix, stillToSortSuffix, None),
      solved,
      context.providedOrderFactory.providedOrder(orderColumns, ProvidedOrder.Left),
      context
    )
    markOrderAsLeveragedBackwardsUntilOrigin(plan, context.providedOrderFactory)
    plan
  }

  def planShortestRelationship(
    inner: LogicalPlan,
    shortestRelationship: ShortestRelationshipPattern,
    nodePredicates: Set[VariablePredicate],
    relPredicates: Set[VariablePredicate],
    pathPredicates: Set[Expression],
    solvedPredicates: Set[Expression],
    withFallBack: Boolean,
    disallowSameNode: Boolean = true,
    context: LogicalPlanningContext
  ): LogicalPlan = {

    val solved = solveds.get(inner.id).asSinglePlannerQuery.amendQueryGraph(
      _.addShortestRelationship(shortestRelationship).addPredicates(solvedPredicates.toSeq: _*)
    )

    val (rewrittenRelationshipPredicates, rewrittenNodePredicates, rewrittenSource) =
      solveSubqueryExpressionsForExtractedPredicates(inner, nodePredicates, relPredicates, context)

    annotate(
      FindShortestPaths(
        rewrittenSource,
        shortestRelationship,
        rewrittenNodePredicates.toSeq,
        rewrittenRelationshipPredicates.toSeq,
        pathPredicates.toSeq,
        withFallBack,
        disallowSameNode
      ),
      solved,
      providedOrders.get(rewrittenSource.id).fromLeft,
      context
    )
  }

  @deprecated(
    "Uses the old shortest path implementation. Available for feature toggle. use planShortestRelationship instead.",
    since = "5.3.0"
  )
  def planLegacyShortestRelationship(
    inner: LogicalPlan,
    shortestRelationship: ShortestRelationshipPattern,
    predicates: Seq[Expression],
    withFallBack: Boolean,
    disallowSameNode: Boolean = true,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.amendQueryGraph(
      _.addShortestRelationship(shortestRelationship).addPredicates(predicates: _*)
    )
    val (rewrittenPredicates, rewrittenInner) = SubqueryExpressionSolver.ForMulti.solve(inner, predicates, context)
    annotate(
      LegacyFindShortestPaths(
        rewrittenInner,
        shortestRelationship,
        rewrittenPredicates,
        withFallBack,
        disallowSameNode
      ),
      solved,
      providedOrders.get(rewrittenInner.id).fromLeft,
      context
    )
  }

  def planProjectEndpoints(
    inner: LogicalPlan,
    start: String,
    startInScope: Boolean,
    end: String,
    endInScope: Boolean,
    patternRel: PatternRelationship,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.amendQueryGraph(_.addPatternRelationship(patternRel))
    annotate(
      ProjectEndpoints(
        inner,
        patternRel.name,
        start,
        startInScope,
        end,
        endInScope,
        patternRel.types,
        patternRel.dir,
        patternRel.length
      ),
      solved,
      providedOrders.get(inner.id).fromLeft,
      context
    )
  }

  def planProjectionForUnionMapping(
    inner: LogicalPlan,
    expressions: Map[String, Expression],
    context: LogicalPlanningContext
  ): LogicalPlan = {
    annotate(
      Projection(inner, Set.empty, expressions),
      solveds.get(inner.id),
      providedOrders.get(inner.id).fromLeft,
      context
    )
  }

  def planUnion(
    left: LogicalPlan,
    right: LogicalPlan,
    unionMappings: List[UnionMapping],
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solvedLeft = solveds.get(left.id)
    val solvedRight = solveds.get(right.id).asSinglePlannerQuery
    val solved = UnionQuery(solvedLeft, solvedRight, distinct = false, unionMappings)

    val plan = Union(left, right)
    annotate(plan, solved, ProvidedOrder.empty, context)
  }

  def planOrderedUnion(
    left: LogicalPlan,
    right: LogicalPlan,
    unionMappings: List[UnionMapping],
    sortedColumns: Seq[ColumnOrder],
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solvedLeft = solveds.get(left.id)
    val solvedRight = solveds.get(right.id).asSinglePlannerQuery
    val solved = UnionQuery(solvedLeft, solvedRight, distinct = false, unionMappings)

    val providedOrder = providedOrders.get(left.id).commonPrefixWith(providedOrders.get(right.id)).fromBoth

    val plan = annotate(OrderedUnion(left, right, sortedColumns), solved, providedOrder, context)
    markOrderAsLeveragedBackwardsUntilOrigin(plan, context.providedOrderFactory)
    plan
  }

  def planDistinctForUnion(left: LogicalPlan, context: LogicalPlanningContext): LogicalPlan = {
    val returnAll = left.availableSymbols.map { s => s -> Variable(s)(InputPosition.NONE) }

    val solved = solveds.get(left.id) match {
      case u: UnionQuery => markDistinctInUnion(u)
      case _ => throw new IllegalStateException("Planning a distinct for union, but no union was planned before.")
    }
    if (returnAll.isEmpty) {
      annotate(left.copyPlanWithIdGen(idGen), solved, providedOrders.get(left.id).fromLeft, context)
    } else {
      annotate(Distinct(left, returnAll.toMap), solved, providedOrders.get(left.id).fromLeft, context)
    }
  }

  def planOrderedDistinctForUnion(
    left: LogicalPlan,
    orderToLeverage: Seq[Expression],
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val returnAll = left.availableSymbols.map { s => s -> Variable(s)(InputPosition.NONE) }

    val solved = solveds.get(left.id) match {
      case u: UnionQuery => markDistinctInUnion(u)
      case _ => throw new IllegalStateException("Planning a distinct for or union, but no union was planned before.")
    }
    if (returnAll.isEmpty) {
      annotate(left.copyPlanWithIdGen(idGen), solved, providedOrders.get(left.id).fromLeft, context)
    } else {
      val plan = annotate(
        OrderedDistinct(left, returnAll.toMap, orderToLeverage),
        solved,
        providedOrders.get(left.id).fromLeft,
        context
      )
      markOrderAsLeveragedBackwardsUntilOrigin(plan, context.providedOrderFactory)
      plan
    }
  }

  private def markDistinctInUnion(query: PlannerQuery): PlannerQuery = {
    query match {
      case u @ UnionQuery(lhs, _, _, _) => u.copy(lhs = markDistinctInUnion(lhs), distinct = true)
      case s                            => s
    }
  }

  /**
   *
   * @param expressions must be solved by the ListSubqueryExpressionSolver. This is not done here since that can influence how we plan distinct,
   *                    thus this logic is put into [[distinct]] instead.
   */
  def planDistinct(
    left: LogicalPlan,
    expressions: Map[String, Expression],
    reported: Map[String, Expression],
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved: SinglePlannerQuery =
      solveds.get(left.id).asSinglePlannerQuery.updateTailOrSelf(_.updateQueryProjection(_ =>
        DistinctQueryProjection(reported)
      ))
    val columnsWithRenames = renameProvidedOrderColumns(providedOrders.get(left.id).columns, expressions)
    val providedOrder = context.providedOrderFactory.providedOrder(columnsWithRenames, ProvidedOrder.Left)
    annotate(Distinct(left, expressions), solved, providedOrder, context)
  }

  /**
   *
   * @param expressions must be solved by the ListSubqueryExpressionSolver. This is not done here since that can influence how we plan distinct,
   *                    thus this logic is put into [[distinct]] instead.
   */
  def planOrderedDistinct(
    left: LogicalPlan,
    expressions: Map[String, Expression],
    orderToLeverage: Seq[Expression],
    reported: Map[String, Expression],
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved: SinglePlannerQuery =
      solveds.get(left.id).asSinglePlannerQuery.updateTailOrSelf(_.updateQueryProjection(_ =>
        DistinctQueryProjection(reported)
      ))
    val columnsWithRenames = renameProvidedOrderColumns(providedOrders.get(left.id).columns, expressions)
    val providedOrder = context.providedOrderFactory.providedOrder(columnsWithRenames, ProvidedOrder.Left)
    val plan = annotate(OrderedDistinct(left, expressions, orderToLeverage), solved, providedOrder, context)
    markOrderAsLeveragedBackwardsUntilOrigin(plan, context.providedOrderFactory)
    plan
  }

  def updateSolvedForOr(
    orPlan: LogicalPlan,
    solvedQueryGraph: QueryGraph,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(orPlan.id) match {
      case UnionQuery(lhs, rhs, _, _) => rhs.updateTailOrSelf { that =>
          val newHints = lhs.allHints ++ rhs.allHints
          that.withQueryGraph(solvedQueryGraph.withHints(newHints))
        }
      case q => throw new IllegalStateException(s"Expected orPlan to solve a UnionQuery, got: $q")
    }
    val cardinality = cardinalityModel(
      solved,
      context.plannerState.input.labelInfo,
      context.plannerState.input.relTypeInfo,
      context.semanticTable,
      context.plannerState.indexCompatiblePredicatesProviderContext
    )
    // Change solved and cardinality
    val keptAttributes = Attributes(idGen, providedOrders, leveragedOrders, labelAndRelTypeInfos)
    val newPlan = orPlan.copyPlanWithIdGen(keptAttributes.copy(orPlan.id))
    solveds.set(newPlan.id, solved)
    cardinalities.set(newPlan.id, cardinality)
    newPlan
  }

  def planTriadicSelection(
    positivePredicate: Boolean,
    left: LogicalPlan,
    sourceId: String,
    seenId: String,
    targetId: String,
    right: LogicalPlan,
    predicate: Expression,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = {
      val leftSolved = solveds.get(left.id).asSinglePlannerQuery
      val rightSolved = solveds.get(right.id).asSinglePlannerQuery.amendQueryGraph(_.withoutArguments())
      (leftSolved ++ rightSolved).updateTailOrSelf(_.amendQueryGraph(_.addPredicates(predicate)))
    }
    annotate(
      TriadicSelection(left, right, positivePredicate, sourceId, seenId, targetId),
      solved,
      providedOrders.get(left.id).fromLeft,
      context
    )
  }

  def planCreate(inner: LogicalPlan, pattern: CreatePattern, context: LogicalPlanningContext): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.amendQueryGraph(_.addMutatingPatterns(pattern))
    val (rewrittenPattern, rewrittenInner) = SubqueryExpressionSolver.ForMappable().solve(inner, pattern, context)
    val plan = plans.Create(rewrittenInner, rewrittenPattern.nodes, rewrittenPattern.relationships)
    val providedOrder = providedOrderOfUpdate(plan, rewrittenInner, context.settings.executionModel)
    annotate(plan, solved, providedOrder, context)
  }

  def planMerge(
    inner: LogicalPlan,
    createNodePatterns: Seq[CreateNode],
    createRelationshipPatterns: Seq[CreateRelationship],
    onMatchPatterns: Seq[SetMutatingPattern],
    onCreatePatterns: Seq[SetMutatingPattern],
    nodesToLock: Set[String],
    context: LogicalPlanningContext
  ): Merge = {

    val patterns =
      if (createRelationshipPatterns.isEmpty) {
        MergeNodePattern(
          createNodePatterns.head,
          solveds(inner.id).asSinglePlannerQuery.queryGraph,
          onCreatePatterns,
          onMatchPatterns
        )
      } else {
        MergeRelationshipPattern(
          createNodePatterns,
          createRelationshipPatterns,
          solveds(inner.id).asSinglePlannerQuery.queryGraph,
          onCreatePatterns,
          onMatchPatterns
        )
      }
    val rewrittenNodePatterns =
      createNodePatterns.map(p => SubqueryExpressionSolver.ForMappable().solve(inner, p, context)._1)
    val rewrittenRelPatterns =
      createRelationshipPatterns.map(p => SubqueryExpressionSolver.ForMappable().solve(inner, p, context)._1)

    val solved = RegularSinglePlannerQuery().amendQueryGraph(_.addMutatingPatterns(patterns))
    val merge =
      Merge(inner, rewrittenNodePatterns, rewrittenRelPatterns, onMatchPatterns, onCreatePatterns, nodesToLock)
    val providedOrder = providedOrderOfUpdate(merge, inner, context.settings.executionModel)
    annotate(merge, solved, providedOrder, context)
  }

  def planConditionalApply(
    lhs: LogicalPlan,
    rhs: LogicalPlan,
    idNames: Seq[String],
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(lhs.id).asSinglePlannerQuery ++ solveds.get(rhs.id).asSinglePlannerQuery
    val providedOrder = providedOrderOfApply(lhs, rhs, context.settings.executionModel)
    annotate(ConditionalApply(lhs, rhs, idNames), solved, providedOrder, context)
  }

  def planAntiConditionalApply(
    lhs: LogicalPlan,
    rhs: LogicalPlan,
    idNames: Seq[String],
    context: LogicalPlanningContext,
    maybeSolved: Option[SinglePlannerQuery] = None
  ): LogicalPlan = {
    val solved =
      maybeSolved.getOrElse(solveds.get(lhs.id).asSinglePlannerQuery ++ solveds.get(rhs.id).asSinglePlannerQuery)
    val providedOrder = providedOrderOfApply(lhs, rhs, context.settings.executionModel)
    annotate(AntiConditionalApply(lhs, rhs, idNames), solved, providedOrder, context)
  }

  def planDeleteNode(inner: LogicalPlan, delete: DeleteExpression, context: LogicalPlanningContext): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.amendQueryGraph(_.addMutatingPatterns(delete))
    val (rewrittenDelete, rewrittenInner) = SubqueryExpressionSolver.ForMappable().solve(inner, delete, context)
    val plan =
      if (delete.forced) {
        DetachDeleteNode(rewrittenInner, rewrittenDelete.expression)
      } else {
        DeleteNode(rewrittenInner, rewrittenDelete.expression)
      }
    val providedOrder = providedOrderOfUpdate(plan, inner, context.settings.executionModel)
    annotate(plan, solved, providedOrder, context)
  }

  def planDeleteRelationship(
    inner: LogicalPlan,
    delete: DeleteExpression,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.amendQueryGraph(_.addMutatingPatterns(delete))
    val (rewrittenDelete, rewrittenInner) = SubqueryExpressionSolver.ForMappable().solve(inner, delete, context)
    val plan = DeleteRelationship(rewrittenInner, rewrittenDelete.expression)
    val providedOrder = providedOrderOfUpdate(plan, inner, context.settings.executionModel)
    annotate(plan, solved, providedOrder, context)
  }

  def planDeletePath(inner: LogicalPlan, delete: DeleteExpression, context: LogicalPlanningContext): LogicalPlan = {
    // `delete.expression` can only be a PathExpression, ListSubqueryExpressionSolver not needed
    val solved = solveds.get(inner.id).asSinglePlannerQuery.amendQueryGraph(_.addMutatingPatterns(delete))

    val plan =
      if (delete.forced) {
        DetachDeletePath(inner, delete.expression)
      } else {
        DeletePath(inner, delete.expression)
      }
    val providedOrder = providedOrderOfUpdate(plan, inner, context.settings.executionModel)
    annotate(plan, solved, providedOrder, context)
  }

  def planDeleteExpression(
    inner: LogicalPlan,
    delete: DeleteExpression,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.amendQueryGraph(_.addMutatingPatterns(delete))
    val (rewrittenDelete, rewrittenInner) = SubqueryExpressionSolver.ForMappable().solve(inner, delete, context)
    val plan =
      if (delete.forced) {
        DetachDeleteExpression(rewrittenInner, rewrittenDelete.expression)
      } else {
        plans.DeleteExpression(rewrittenInner, rewrittenDelete.expression)
      }
    val providedOrder = providedOrderOfUpdate(plan, inner, context.settings.executionModel)
    annotate(plan, solved, providedOrder, context)
  }

  def planSetLabel(inner: LogicalPlan, pattern: SetLabelPattern, context: LogicalPlanningContext): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.amendQueryGraph(_.addMutatingPatterns(pattern))
    val plan = SetLabels(inner, pattern.idName, pattern.labels.toSet)
    val providedOrder = providedOrderOfUpdate(plan, inner, context.settings.executionModel)
    annotate(plan, solved, providedOrder, context)
  }

  def planSetNodeProperty(
    inner: LogicalPlan,
    pattern: SetNodePropertyPattern,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.amendQueryGraph(_.addMutatingPatterns(pattern))
    val (rewrittenPattern, rewrittenInner) = SubqueryExpressionSolver.ForMappable().solve(inner, pattern, context)
    val plan = SetNodeProperty(
      rewrittenInner,
      rewrittenPattern.idName,
      rewrittenPattern.propertyKey,
      rewrittenPattern.expression
    )
    val providedOrder = providedOrderOfUpdate(plan, rewrittenInner, context.settings.executionModel)
    annotate(plan, solved, providedOrder, context)
  }

  def planSetNodeProperties(
    inner: LogicalPlan,
    pattern: SetNodePropertiesPattern,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.amendQueryGraph(_.addMutatingPatterns(pattern))
    val (rewrittenPattern, rewrittenInner) = SubqueryExpressionSolver.ForMappable().solve(inner, pattern, context)
    val plan = SetNodeProperties(rewrittenInner, rewrittenPattern.idName, rewrittenPattern.items)
    val providedOrder = providedOrderOfUpdate(plan, rewrittenInner, context.settings.executionModel)
    annotate(plan, solved, providedOrder, context)
  }

  def planSetNodePropertiesFromMap(
    inner: LogicalPlan,
    pattern: SetNodePropertiesFromMapPattern,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.amendQueryGraph(_.addMutatingPatterns(pattern))
    val (rewrittenPattern, rewrittenInner) = SubqueryExpressionSolver.ForMappable().solve(inner, pattern, context)
    val plan = SetNodePropertiesFromMap(
      rewrittenInner,
      rewrittenPattern.idName,
      rewrittenPattern.expression,
      rewrittenPattern.removeOtherProps
    )
    val providedOrder = providedOrderOfUpdate(plan, rewrittenInner, context.settings.executionModel)
    annotate(plan, solved, providedOrder, context)
  }

  def planSetRelationshipProperty(
    inner: LogicalPlan,
    pattern: SetRelationshipPropertyPattern,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.amendQueryGraph(_.addMutatingPatterns(pattern))
    val (rewrittenPattern, rewrittenInner) = SubqueryExpressionSolver.ForMappable().solve(inner, pattern, context)
    val plan = SetRelationshipProperty(
      rewrittenInner,
      rewrittenPattern.idName,
      rewrittenPattern.propertyKey,
      rewrittenPattern.expression
    )
    val providedOrder = providedOrderOfUpdate(plan, rewrittenInner, context.settings.executionModel)
    annotate(plan, solved, providedOrder, context)
  }

  def planSetRelationshipProperties(
    inner: LogicalPlan,
    pattern: SetRelationshipPropertiesPattern,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.amendQueryGraph(_.addMutatingPatterns(pattern))
    val (rewrittenPattern, rewrittenInner) = SubqueryExpressionSolver.ForMappable().solve(inner, pattern, context)
    val plan = SetRelationshipProperties(rewrittenInner, rewrittenPattern.idName, rewrittenPattern.items)
    val providedOrder = providedOrderOfUpdate(plan, rewrittenInner, context.settings.executionModel)
    annotate(plan, solved, providedOrder, context)
  }

  def planSetRelationshipPropertiesFromMap(
    inner: LogicalPlan,
    pattern: SetRelationshipPropertiesFromMapPattern,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.amendQueryGraph(_.addMutatingPatterns(pattern))
    val (rewrittenPattern, rewrittenInner) = SubqueryExpressionSolver.ForMappable().solve(inner, pattern, context)
    val plan = SetRelationshipPropertiesFromMap(
      rewrittenInner,
      rewrittenPattern.idName,
      rewrittenPattern.expression,
      rewrittenPattern.removeOtherProps
    )
    val providedOrder = providedOrderOfUpdate(plan, rewrittenInner, context.settings.executionModel)
    annotate(plan, solved, providedOrder, context)
  }

  def planSetPropertiesFromMap(
    inner: LogicalPlan,
    pattern: SetPropertiesFromMapPattern,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.amendQueryGraph(_.addMutatingPatterns(pattern))
    val (rewrittenPattern, rewrittenInner) = SubqueryExpressionSolver.ForMappable().solve(inner, pattern, context)
    val plan = SetPropertiesFromMap(
      rewrittenInner,
      rewrittenPattern.entityExpression,
      rewrittenPattern.expression,
      rewrittenPattern.removeOtherProps
    )
    val providedOrder = providedOrderOfUpdate(plan, rewrittenInner, context.settings.executionModel)
    annotate(plan, solved, providedOrder, context)
  }

  def planSetProperty(inner: LogicalPlan, pattern: SetPropertyPattern, context: LogicalPlanningContext): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.amendQueryGraph(_.addMutatingPatterns(pattern))
    val (rewrittenPattern, rewrittenInner) = SubqueryExpressionSolver.ForMappable().solve(inner, pattern, context)
    val plan = SetProperty(
      rewrittenInner,
      rewrittenPattern.entityExpression,
      rewrittenPattern.propertyKeyName,
      rewrittenPattern.expression
    )
    val providedOrder = providedOrderOfUpdate(plan, rewrittenInner, context.settings.executionModel)
    annotate(plan, solved, providedOrder, context)
  }

  def planSetProperties(
    inner: LogicalPlan,
    pattern: SetPropertiesPattern,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.amendQueryGraph(_.addMutatingPatterns(pattern))
    val (rewrittenPattern, rewrittenInner) = SubqueryExpressionSolver.ForMappable().solve(inner, pattern, context)
    val plan = SetProperties(rewrittenInner, rewrittenPattern.entityExpression, rewrittenPattern.items)
    val providedOrder = providedOrderOfUpdate(plan, rewrittenInner, context.settings.executionModel)
    annotate(plan, solved, providedOrder, context)
  }

  def planRemoveLabel(inner: LogicalPlan, pattern: RemoveLabelPattern, context: LogicalPlanningContext): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.amendQueryGraph(_.addMutatingPatterns(pattern))
    val plan = RemoveLabels(inner, pattern.idName, pattern.labels.toSet)
    val providedOrder = providedOrderOfUpdate(plan, inner, context.settings.executionModel)
    annotate(plan, solved, providedOrder, context)
  }

  def planForeachApply(
    left: LogicalPlan,
    innerUpdates: LogicalPlan,
    pattern: ForeachPattern,
    context: LogicalPlanningContext,
    expression: Expression
  ): LogicalPlan = {
    val solved = solveds.get(left.id).asSinglePlannerQuery.amendQueryGraph(_.addMutatingPatterns(pattern))
    val (rewrittenExpression, rewrittenLeft) = SubqueryExpressionSolver.ForSingle.solve(left, expression, context)
    val plan = ForeachApply(rewrittenLeft, innerUpdates, pattern.variable, rewrittenExpression)
    val providedOrder = providedOrderOfApply(rewrittenLeft, innerUpdates, context.settings.executionModel)
    annotate(plan, solved, providedOrder, context)
  }

  def planForeach(
    inner: LogicalPlan,
    pattern: ForeachPattern,
    context: LogicalPlanningContext,
    expression: Expression,
    mutations: collection.Seq[SimpleMutatingPattern]
  ): LogicalPlan = {
    val solved = solveds.get(inner.id).asSinglePlannerQuery.amendQueryGraph(_.addMutatingPatterns(pattern))
    val (rewrittenExpression, rewrittenLeft) = SubqueryExpressionSolver.ForSingle.solve(inner, expression, context)
    val plan = Foreach(rewrittenLeft, pattern.variable, rewrittenExpression, mutations)
    val providedOrder = providedOrderOfUpdate(plan, inner, context.settings.executionModel)
    annotate(plan, solved, providedOrder, context)
  }

  def planEager(
    inner: LogicalPlan,
    context: LogicalPlanningContext,
    reasons: ListSet[EagernessReason.Reason]
  ): LogicalPlan =
    annotate(Eager(inner, reasons), solveds.get(inner.id), providedOrders.get(inner.id).fromLeft, context)

  def planError(
    inner: LogicalPlan,
    exception: ExhaustiveShortestPathForbiddenException,
    context: LogicalPlanningContext
  ): LogicalPlan =
    annotate(ErrorPlan(inner, exception), solveds.get(inner.id), providedOrders.get(inner.id).fromLeft, context)

  /**
   * @param lastInterestingOrders the interesting order of the last part of the whole query, or `None` for UNION queries.
   */
  def planProduceResult(
    inner: LogicalPlan,
    columns: Seq[String],
    lastInterestingOrders: Option[InterestingOrder],
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val produceResult = ProduceResult(inner, columns)
    if (columns.nonEmpty) {
      val newSolved = solveds.get(inner.id) match {
        case query: SinglePlannerQuery => query.updateTailOrSelf(
            _.updateQueryProjection(_.withIsTerminating(true))
          )
        case uq @ UnionQuery(_, _, _, _) =>
          uq
      }
      solveds.set(produceResult.id, newSolved)
    } else {
      solveds.copy(inner.id, produceResult.id)
    }
    // Do not calculate cardinality for ProduceResult. Since the passed context does not have accurate label information
    // It will get a wrong value with some projections. Use the cardinality of inner instead
    cardinalities.copy(inner.id, produceResult.id)
    providedOrders.set(produceResult.id, providedOrders.get(inner.id).fromLeft)

    if (lastInterestingOrders.exists(_.requiredOrderCandidate.nonEmpty)) {
      markOrderAsLeveragedBackwardsUntilOrigin(produceResult, context.providedOrderFactory)
    }

    produceResult
  }

  def addMissingStandaloneArgumentPatternNodes(
    plan: LogicalPlan,
    query: SinglePlannerQuery,
    context: LogicalPlanningContext
  ): LogicalPlan = {
    val solved = solveds.get(plan.id).asSinglePlannerQuery
    val missingNodes = query.queryGraph.standaloneArgumentPatternNodes diff solved.queryGraph.patternNodes
    if (missingNodes.isEmpty) {
      plan
    } else {
      val newSolved = solved.amendQueryGraph(_.addPatternNodes(missingNodes.toSeq: _*))
      val providedOrder = providedOrders.get(plan.id)
      annotate(plan.copyPlanWithIdGen(idGen), newSolved, providedOrder, context)
    }
  }

  /**
   * Updates may make the current provided order invalid since the order may depend on something that gets mutated.
   * If this is the case, this method returns an empty provided order, otherwise it forwards the provided order from the left.
   */
  private def providedOrderOfUpdate(
    updatePlan: UpdatingPlan,
    sourcePlan: LogicalPlan,
    executionModel: ExecutionModel
  ): ProvidedOrder =
    if (invalidatesProvidedOrder(updatePlan, executionModel)) {
      ProvidedOrder.empty
    } else {
      providedOrders.get(sourcePlan.id).fromLeft
    }

  /**
   * Plans with an empty argument on the lhs maintains the ordering of the rhs. In that case, this method returns the order coming from the rhs.
   * Plans with a rhs may invalidate the provided order coming from the lhs. If this is the case, this method returns an empty provided order.
   * In any other case (i.e. lhs is not a single argument, and rhs is not invalidating lhs ordering), it forwards the provided order from the left.
   */
  private def providedOrderOfApply(
    left: LogicalPlan,
    right: LogicalPlan,
    executionModel: ExecutionModel
  ): ProvidedOrder = {
    (left, right) match {
      case (Argument(args), rhs) if args.isEmpty =>
        providedOrders.get(rhs.id).fromRight
      case (_, rhs) if invalidatesProvidedOrderRecursive(rhs, executionModel) =>
        ProvidedOrder.empty
      case (lhs, _) => // If the LHS has duplicate values, we cannot guarantee any added order from the RHS
        providedOrders.get(lhs.id).fromLeft
    }
  }

  private def assertRhsDoesNotInvalidateLhsOrder(
    plan: LogicalPlan,
    providedOrder: ProvidedOrder,
    executionModel: ExecutionModel
  ): Unit = {
    if (AssertionRunner.ASSERTIONS_ENABLED) {
      (plan.lhs, plan.rhs, providedOrder.orderOrigin) match {
        case (Some(left), Some(right), Some(ProvidedOrder.Left))
          if invalidatesProvidedOrderRecursive(right, executionModel) =>
          val msg =
            s"LHS claims to provide an order, but RHS contains clauses that invalidates this order.\nProvided order: $providedOrder\nLHS: $left\nRHS: $right"
          throw new AssertionError(msg)
        case _ =>
      }
    }
  }

  /**
   * Currently we consider all updates, except MERGE, as invalidating provided order
   */
  private def invalidatesProvidedOrder(plan: LogicalPlan, executionModel: ExecutionModel): Boolean = {
    (plan match {
      // MERGE will either be ordered by its inner plan or create a single row which by
      // definition is ordered. However if you do ON MATCH SET ... that might invalidate the
      // inner ordering.
      case m: Merge        => m.onMatch.nonEmpty
      case _: UpdatingPlan => true
      case _               => false
    }) || executionModel.invalidatesProvidedOrder(plan)
  }

  private def invalidatesProvidedOrderRecursive(plan: LogicalPlan, executionModel: ExecutionModel): Boolean =
    plan.folder.treeExists { case plan: LogicalPlan if invalidatesProvidedOrder(plan, executionModel) => true }

  /**
   * Compute cardinality for a plan. Set this cardinality in the Cardinalities attribute.
   * Set the other attributes with the provided arguments (solved and providedOrder).
   *
   * @return the same plan
   */
  private def annotate[T <: LogicalPlan](
    plan: T,
    solved: PlannerQuery,
    providedOrder: ProvidedOrder,
    context: LogicalPlanningContext
  ): T = {
    assertNoBadExpressionsExists(plan)
    assertRhsDoesNotInvalidateLhsOrder(plan, providedOrder, context.settings.executionModel)
    val cardinality =
      cardinalityModel(
        solved,
        context.plannerState.input.labelInfo,
        context.plannerState.input.relTypeInfo,
        context.semanticTable,
        context.plannerState.indexCompatiblePredicatesProviderContext
      )
    solveds.set(plan.id, solved)
    cardinalities.set(plan.id, cardinality)
    providedOrders.set(plan.id, providedOrder)
    plan
  }

  /**
   * Same as [[annotate()]], but in addition also set the labelAndRelTypeInfos attribute.
   *
   * @return the same plan
   */
  private def annotateSelection(
    selection: Selection,
    solved: PlannerQuery,
    providedOrder: ProvidedOrder,
    context: LogicalPlanningContext
  ): Selection = {
    labelAndRelTypeInfos.set(
      selection.id,
      Some(LabelAndRelTypeInfo(context.plannerState.input.labelInfo, context.plannerState.input.relTypeInfo))
    )

    annotate(selection, solved, providedOrder, context)
  }

  /**
   * There probably exists some type level way of achieving this with type safety instead of manually searching through the expression tree like this
   */
  private def assertNoBadExpressionsExists(root: Any): Unit = {
    checkOnlyWhenAssertionsAreEnabled(!root.folder.treeExists {
      case _: PatternComprehension | _: PatternExpression | _: IRExpression | _: MapProjection =>
        throw new InternalException(s"This expression should not be added to a logical plan:\n$root")
      case _ =>
        false
    })
  }

  private def projectedDirection(
    pattern: PatternRelationship,
    from: String,
    dir: SemanticDirection
  ): SemanticDirection = {
    if (dir == SemanticDirection.BOTH) {
      if (from == pattern.left) {
        SemanticDirection.OUTGOING
      } else {
        SemanticDirection.INCOMING
      }
    } else {
      pattern.dir
    }
  }

  private def planRegularProjectionHelper(
    inner: LogicalPlan,
    discardSymbols: Set[String],
    expressions: Map[String, Expression],
    context: LogicalPlanningContext,
    solved: SinglePlannerQuery
  ) = {
    val columnsWithRenames = renameProvidedOrderColumns(providedOrders.get(inner.id).columns, expressions)
    val providedOrder = context.providedOrderFactory.providedOrder(columnsWithRenames, ProvidedOrder.Left)
    annotate(Projection(inner, discardSymbols, expressions), solved, providedOrder, context)
  }

  /**
   * The provided order is used to describe the current ordering of the LogicalPlan within a complete plan tree. For
   * index leaf operators this can be planned as an IndexOrder for the index to provide. In that case it only works
   * if all columns are sorted in the same direction, so we need to narrow the scope for these index operations.
   */
  private def toIndexOrder(providedOrder: ProvidedOrder): IndexOrder = providedOrder match {
    case ProvidedOrder.empty                                           => IndexOrderNone
    case ProvidedOrder(columns) if columns.forall(c => c.isAscending)  => IndexOrderAscending
    case ProvidedOrder(columns) if columns.forall(c => !c.isAscending) => IndexOrderDescending
    case _ => throw new IllegalStateException("Cannot mix ascending and descending columns when using index order")
  }

  /**
   * Rename sort columns if they are renamed in a projection.
   */
  private def renameProvidedOrderColumns(
    columns: Seq[ordering.ColumnOrder],
    projectExpressions: Map[String, Expression]
  ): Seq[ordering.ColumnOrder] = {
    columns.map {
      case columnOrder @ ordering.ColumnOrder(e @ Property(v @ Variable(varName), p @ PropertyKeyName(propName))) =>
        projectExpressions.collectFirst {
          case (
              newName,
              Property(Variable(`varName`), PropertyKeyName(`propName`)) | CachedProperty(
                `varName`,
                _,
                PropertyKeyName(`propName`),
                _,
                _
              )
            ) =>
            ordering.ColumnOrder(Variable(newName)(v.position), columnOrder.isAscending)
          case (newName, Variable(`varName`)) =>
            ordering.ColumnOrder(
              Property(Variable(newName)(v.position), PropertyKeyName(propName)(p.position))(e.position),
              columnOrder.isAscending
            )
        }.getOrElse(columnOrder)
      case columnOrder @ ordering.ColumnOrder(expression) =>
        projectExpressions.collectFirst {
          case (newName, `expression`) =>
            ordering.ColumnOrder(Variable(newName)(expression.position), columnOrder.isAscending)
        }.getOrElse(columnOrder)
    }
  }

  private def trimAndRenameProvidedOrder(
    providedOrder: ProvidedOrder,
    grouping: Map[String, Expression]
  ): Seq[ordering.ColumnOrder] = {
    // Trim provided order for each sort column, if it is a non-grouping column
    val trimmed = providedOrder.columns.takeWhile {
      case ordering.ColumnOrder(Property(Variable(varName), PropertyKeyName(propName))) =>
        grouping.values.exists {
          case CachedProperty(`varName`, _, PropertyKeyName(`propName`), _, _)    => true
          case CachedHasProperty(`varName`, _, PropertyKeyName(`propName`), _, _) => true
          case Property(Variable(`varName`), PropertyKeyName(`propName`))         => true
          case _                                                                  => false
        }
      case ordering.ColumnOrder(expression) =>
        grouping.values.exists {
          case `expression` => true
          case _            => false
        }
    }
    renameProvidedOrderColumns(trimmed, grouping)
  }

  /**
   * Starting from `lp`, traverse the logical plan backwards until finding the origin(s) of the current provided order.
   * For each plan on the way, set `leveragedOrder` to `true`.
   *
   * @param lp the plan that leverages a provided order. Must be an already annotated plan.
   */
  private def markOrderAsLeveragedBackwardsUntilOrigin(
    lp: LogicalPlan,
    providedOrderFactory: ProvidedOrderFactory
  ): Unit = {
    leveragedOrders.set(lp.id, true)

    def loop(current: LogicalPlan): Unit = {
      leveragedOrders.set(current.id, true)
      val origin = providedOrders.get(current.id).orderOrigin
      origin match {
        case Some(ProvidedOrder.Left)  => loop(current.lhs.get)
        case Some(ProvidedOrder.Right) => loop(current.rhs.get)
        case Some(ProvidedOrder.Both)  => loop(current.lhs.get); loop(current.rhs.get)
        case Some(ProvidedOrder.Self)  => // done
        case None                      =>
          // If the executionModel doesn't provide order ending up here is expected
          AssertMacros.checkOnlyWhenAssertionsAreEnabled(
            !providedOrderFactory.assertOnNoProvidedOrder,
            s"While marking leveraged order we encountered a plan with no provided order:\n ${LogicalPlanToPlanBuilderString(current)}"
          )
      }
    }

    providedOrders.get(lp.id).orderOrigin match {
      case Some(ProvidedOrder.Left)  => lp.lhs.foreach(loop)
      case Some(ProvidedOrder.Right) => lp.rhs.foreach(loop)
      case Some(ProvidedOrder.Both)  => lp.lhs.foreach(loop); lp.rhs.foreach(loop)
      case Some(
          ProvidedOrder.Self
        ) => // If the plan both introduces and leverages the order, we do not want to traverse into the children
      case None =>
        // The plan itself leverages the order, but does not maintain it.
        // Currently, in that case we assume it is a one-child plan,
        // since at the time of writing there is no two child plan that leverages and destroys ordering
        lp.lhs.foreach(loop)
        AssertMacros.checkOnlyWhenAssertionsAreEnabled(
          lp.rhs.isEmpty,
          "We assume that there is no two-child plan leveraging but destroying ordering."
        )
    }
  }
}
