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
package org.neo4j.cypher.internal.parser.v5;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * A ParseTreeListener for Cypher5Parser.
 * Generated by org.neo4j.cypher.internal.parser.GenerateListenerMavenPlugin
 */
public interface Cypher5ParserListener extends ParseTreeListener {
    void exitStatements(Cypher5Parser.StatementsContext ctx);

    void exitStatement(Cypher5Parser.StatementContext ctx);

    void exitPeriodicCommitQueryHintFailure(Cypher5Parser.PeriodicCommitQueryHintFailureContext ctx);

    void exitRegularQuery(Cypher5Parser.RegularQueryContext ctx);

    void exitSingleQuery(Cypher5Parser.SingleQueryContext ctx);

    void exitClause(Cypher5Parser.ClauseContext ctx);

    void exitUseClause(Cypher5Parser.UseClauseContext ctx);

    void exitGraphReference(Cypher5Parser.GraphReferenceContext ctx);

    void exitFinishClause(Cypher5Parser.FinishClauseContext ctx);

    void exitReturnClause(Cypher5Parser.ReturnClauseContext ctx);

    void exitReturnBody(Cypher5Parser.ReturnBodyContext ctx);

    void exitReturnItem(Cypher5Parser.ReturnItemContext ctx);

    void exitReturnItems(Cypher5Parser.ReturnItemsContext ctx);

    void exitOrderItem(Cypher5Parser.OrderItemContext ctx);

    void exitAscToken(Cypher5Parser.AscTokenContext ctx);

    void exitDescToken(Cypher5Parser.DescTokenContext ctx);

    void exitOrderBy(Cypher5Parser.OrderByContext ctx);

    void exitSkip(Cypher5Parser.SkipContext ctx);

    void exitLimit(Cypher5Parser.LimitContext ctx);

    void exitWhereClause(Cypher5Parser.WhereClauseContext ctx);

    void exitWithClause(Cypher5Parser.WithClauseContext ctx);

    void exitCreateClause(Cypher5Parser.CreateClauseContext ctx);

    void exitInsertClause(Cypher5Parser.InsertClauseContext ctx);

    void exitSetClause(Cypher5Parser.SetClauseContext ctx);

    void exitSetItem(Cypher5Parser.SetItemContext ctx);

    void exitRemoveClause(Cypher5Parser.RemoveClauseContext ctx);

    void exitRemoveItem(Cypher5Parser.RemoveItemContext ctx);

    void exitDeleteClause(Cypher5Parser.DeleteClauseContext ctx);

    void exitMatchClause(Cypher5Parser.MatchClauseContext ctx);

    void exitMatchMode(Cypher5Parser.MatchModeContext ctx);

    void exitHint(Cypher5Parser.HintContext ctx);

    void exitMergeClause(Cypher5Parser.MergeClauseContext ctx);

    void exitMergeAction(Cypher5Parser.MergeActionContext ctx);

    void exitUnwindClause(Cypher5Parser.UnwindClauseContext ctx);

    void exitCallClause(Cypher5Parser.CallClauseContext ctx);

    void exitProcedureName(Cypher5Parser.ProcedureNameContext ctx);

    void exitProcedureArgument(Cypher5Parser.ProcedureArgumentContext ctx);

    void exitProcedureResultItem(Cypher5Parser.ProcedureResultItemContext ctx);

    void exitLoadCSVClause(Cypher5Parser.LoadCSVClauseContext ctx);

    void exitForeachClause(Cypher5Parser.ForeachClauseContext ctx);

    void exitSubqueryClause(Cypher5Parser.SubqueryClauseContext ctx);

    void exitSubqueryInTransactionsParameters(Cypher5Parser.SubqueryInTransactionsParametersContext ctx);

    void exitSubqueryInTransactionsBatchParameters(Cypher5Parser.SubqueryInTransactionsBatchParametersContext ctx);

    void exitSubqueryInTransactionsErrorParameters(Cypher5Parser.SubqueryInTransactionsErrorParametersContext ctx);

    void exitSubqueryInTransactionsReportParameters(Cypher5Parser.SubqueryInTransactionsReportParametersContext ctx);

    void exitPatternList(Cypher5Parser.PatternListContext ctx);

    void exitInsertPatternList(Cypher5Parser.InsertPatternListContext ctx);

    void exitPattern(Cypher5Parser.PatternContext ctx);

    void exitInsertPattern(Cypher5Parser.InsertPatternContext ctx);

    void exitQuantifier(Cypher5Parser.QuantifierContext ctx);

    void exitAnonymousPattern(Cypher5Parser.AnonymousPatternContext ctx);

    void exitShortestPathPattern(Cypher5Parser.ShortestPathPatternContext ctx);

    void exitPatternElement(Cypher5Parser.PatternElementContext ctx);

    void exitSelector(Cypher5Parser.SelectorContext ctx);

    void exitGroupToken(Cypher5Parser.GroupTokenContext ctx);

    void exitPathToken(Cypher5Parser.PathTokenContext ctx);

    void exitPathPatternNonEmpty(Cypher5Parser.PathPatternNonEmptyContext ctx);

    void exitNodePattern(Cypher5Parser.NodePatternContext ctx);

    void exitInsertNodePattern(Cypher5Parser.InsertNodePatternContext ctx);

    void exitParenthesizedPath(Cypher5Parser.ParenthesizedPathContext ctx);

    void exitNodeLabels(Cypher5Parser.NodeLabelsContext ctx);

    void exitNodeLabelsIs(Cypher5Parser.NodeLabelsIsContext ctx);

    void exitDynamicExpression(Cypher5Parser.DynamicExpressionContext ctx);

    void exitDynamicLabelType(Cypher5Parser.DynamicLabelTypeContext ctx);

    void exitLabelType(Cypher5Parser.LabelTypeContext ctx);

    void exitRelType(Cypher5Parser.RelTypeContext ctx);

    void exitLabelOrRelType(Cypher5Parser.LabelOrRelTypeContext ctx);

    void exitProperties(Cypher5Parser.PropertiesContext ctx);

    void exitRelationshipPattern(Cypher5Parser.RelationshipPatternContext ctx);

    void exitInsertRelationshipPattern(Cypher5Parser.InsertRelationshipPatternContext ctx);

    void exitLeftArrow(Cypher5Parser.LeftArrowContext ctx);

    void exitArrowLine(Cypher5Parser.ArrowLineContext ctx);

    void exitRightArrow(Cypher5Parser.RightArrowContext ctx);

    void exitPathLength(Cypher5Parser.PathLengthContext ctx);

    void exitLabelExpression(Cypher5Parser.LabelExpressionContext ctx);

    void exitLabelExpression4(Cypher5Parser.LabelExpression4Context ctx);

    void exitLabelExpression4Is(Cypher5Parser.LabelExpression4IsContext ctx);

    void exitLabelExpression3(Cypher5Parser.LabelExpression3Context ctx);

    void exitLabelExpression3Is(Cypher5Parser.LabelExpression3IsContext ctx);

    void exitLabelExpression2(Cypher5Parser.LabelExpression2Context ctx);

    void exitLabelExpression2Is(Cypher5Parser.LabelExpression2IsContext ctx);

    void exitLabelExpression1(Cypher5Parser.LabelExpression1Context ctx);

    void exitLabelExpression1Is(Cypher5Parser.LabelExpression1IsContext ctx);

    void exitInsertNodeLabelExpression(Cypher5Parser.InsertNodeLabelExpressionContext ctx);

    void exitInsertRelationshipLabelExpression(Cypher5Parser.InsertRelationshipLabelExpressionContext ctx);

    void exitExpression(Cypher5Parser.ExpressionContext ctx);

    void exitExpression11(Cypher5Parser.Expression11Context ctx);

    void exitExpression10(Cypher5Parser.Expression10Context ctx);

    void exitExpression9(Cypher5Parser.Expression9Context ctx);

    void exitExpression8(Cypher5Parser.Expression8Context ctx);

    void exitExpression7(Cypher5Parser.Expression7Context ctx);

    void exitComparisonExpression6(Cypher5Parser.ComparisonExpression6Context ctx);

    void exitNormalForm(Cypher5Parser.NormalFormContext ctx);

    void exitExpression6(Cypher5Parser.Expression6Context ctx);

    void exitExpression5(Cypher5Parser.Expression5Context ctx);

    void exitExpression4(Cypher5Parser.Expression4Context ctx);

    void exitExpression3(Cypher5Parser.Expression3Context ctx);

    void exitExpression2(Cypher5Parser.Expression2Context ctx);

    void exitPostFix(Cypher5Parser.PostFixContext ctx);

    void exitProperty(Cypher5Parser.PropertyContext ctx);

    void exitDynamicProperty(Cypher5Parser.DynamicPropertyContext ctx);

    void exitPropertyExpression(Cypher5Parser.PropertyExpressionContext ctx);

    void exitDynamicPropertyExpression(Cypher5Parser.DynamicPropertyExpressionContext ctx);

    void exitExpression1(Cypher5Parser.Expression1Context ctx);

    void exitLiteral(Cypher5Parser.LiteralContext ctx);

    void exitCaseExpression(Cypher5Parser.CaseExpressionContext ctx);

    void exitCaseAlternative(Cypher5Parser.CaseAlternativeContext ctx);

    void exitExtendedCaseExpression(Cypher5Parser.ExtendedCaseExpressionContext ctx);

    void exitExtendedCaseAlternative(Cypher5Parser.ExtendedCaseAlternativeContext ctx);

    void exitExtendedWhen(Cypher5Parser.ExtendedWhenContext ctx);

    void exitListComprehension(Cypher5Parser.ListComprehensionContext ctx);

    void exitPatternComprehension(Cypher5Parser.PatternComprehensionContext ctx);

    void exitReduceExpression(Cypher5Parser.ReduceExpressionContext ctx);

    void exitListItemsPredicate(Cypher5Parser.ListItemsPredicateContext ctx);

    void exitNormalizeFunction(Cypher5Parser.NormalizeFunctionContext ctx);

    void exitTrimFunction(Cypher5Parser.TrimFunctionContext ctx);

    void exitPatternExpression(Cypher5Parser.PatternExpressionContext ctx);

    void exitShortestPathExpression(Cypher5Parser.ShortestPathExpressionContext ctx);

    void exitParenthesizedExpression(Cypher5Parser.ParenthesizedExpressionContext ctx);

    void exitMapProjection(Cypher5Parser.MapProjectionContext ctx);

    void exitMapProjectionElement(Cypher5Parser.MapProjectionElementContext ctx);

    void exitCountStar(Cypher5Parser.CountStarContext ctx);

    void exitExistsExpression(Cypher5Parser.ExistsExpressionContext ctx);

    void exitCountExpression(Cypher5Parser.CountExpressionContext ctx);

    void exitCollectExpression(Cypher5Parser.CollectExpressionContext ctx);

    void exitNumberLiteral(Cypher5Parser.NumberLiteralContext ctx);

    void exitSignedIntegerLiteral(Cypher5Parser.SignedIntegerLiteralContext ctx);

    void exitListLiteral(Cypher5Parser.ListLiteralContext ctx);

    void exitPropertyKeyName(Cypher5Parser.PropertyKeyNameContext ctx);

    void exitParameter(Cypher5Parser.ParameterContext ctx);

    void exitParameterName(Cypher5Parser.ParameterNameContext ctx);

    void exitFunctionInvocation(Cypher5Parser.FunctionInvocationContext ctx);

    void exitFunctionArgument(Cypher5Parser.FunctionArgumentContext ctx);

    void exitFunctionName(Cypher5Parser.FunctionNameContext ctx);

    void exitNamespace(Cypher5Parser.NamespaceContext ctx);

    void exitVariable(Cypher5Parser.VariableContext ctx);

    void exitNonEmptyNameList(Cypher5Parser.NonEmptyNameListContext ctx);

    void exitType(Cypher5Parser.TypeContext ctx);

    void exitTypePart(Cypher5Parser.TypePartContext ctx);

    void exitTypeName(Cypher5Parser.TypeNameContext ctx);

    void exitTypeNullability(Cypher5Parser.TypeNullabilityContext ctx);

    void exitTypeListSuffix(Cypher5Parser.TypeListSuffixContext ctx);

    void exitCommand(Cypher5Parser.CommandContext ctx);

    void exitCreateCommand(Cypher5Parser.CreateCommandContext ctx);

    void exitDropCommand(Cypher5Parser.DropCommandContext ctx);

    void exitShowCommand(Cypher5Parser.ShowCommandContext ctx);

    void exitShowCommandYield(Cypher5Parser.ShowCommandYieldContext ctx);

    void exitYieldItem(Cypher5Parser.YieldItemContext ctx);

    void exitYieldSkip(Cypher5Parser.YieldSkipContext ctx);

    void exitYieldLimit(Cypher5Parser.YieldLimitContext ctx);

    void exitYieldClause(Cypher5Parser.YieldClauseContext ctx);

    void exitCommandOptions(Cypher5Parser.CommandOptionsContext ctx);

    void exitTerminateCommand(Cypher5Parser.TerminateCommandContext ctx);

    void exitComposableCommandClauses(Cypher5Parser.ComposableCommandClausesContext ctx);

    void exitComposableShowCommandClauses(Cypher5Parser.ComposableShowCommandClausesContext ctx);

    void exitShowBriefAndYield(Cypher5Parser.ShowBriefAndYieldContext ctx);

    void exitShowIndexCommand(Cypher5Parser.ShowIndexCommandContext ctx);

    void exitShowIndexesAllowBrief(Cypher5Parser.ShowIndexesAllowBriefContext ctx);

    void exitShowIndexesNoBrief(Cypher5Parser.ShowIndexesNoBriefContext ctx);

    void exitShowConstraintCommand(Cypher5Parser.ShowConstraintCommandContext ctx);

    void exitConstraintAllowYieldType(Cypher5Parser.ConstraintAllowYieldTypeContext ctx);

    void exitConstraintExistType(Cypher5Parser.ConstraintExistTypeContext ctx);

    void exitConstraintBriefAndYieldType(Cypher5Parser.ConstraintBriefAndYieldTypeContext ctx);

    void exitShowConstraintsAllowBriefAndYield(Cypher5Parser.ShowConstraintsAllowBriefAndYieldContext ctx);

    void exitShowConstraintsAllowBrief(Cypher5Parser.ShowConstraintsAllowBriefContext ctx);

    void exitShowConstraintsAllowYield(Cypher5Parser.ShowConstraintsAllowYieldContext ctx);

    void exitShowProcedures(Cypher5Parser.ShowProceduresContext ctx);

    void exitShowFunctions(Cypher5Parser.ShowFunctionsContext ctx);

    void exitFunctionToken(Cypher5Parser.FunctionTokenContext ctx);

    void exitExecutableBy(Cypher5Parser.ExecutableByContext ctx);

    void exitShowFunctionsType(Cypher5Parser.ShowFunctionsTypeContext ctx);

    void exitShowTransactions(Cypher5Parser.ShowTransactionsContext ctx);

    void exitTerminateTransactions(Cypher5Parser.TerminateTransactionsContext ctx);

    void exitShowSettings(Cypher5Parser.ShowSettingsContext ctx);

    void exitSettingToken(Cypher5Parser.SettingTokenContext ctx);

    void exitNamesAndClauses(Cypher5Parser.NamesAndClausesContext ctx);

    void exitStringsOrExpression(Cypher5Parser.StringsOrExpressionContext ctx);

    void exitCommandNodePattern(Cypher5Parser.CommandNodePatternContext ctx);

    void exitCommandRelPattern(Cypher5Parser.CommandRelPatternContext ctx);

    void exitCreateConstraint(Cypher5Parser.CreateConstraintContext ctx);

    void exitConstraintType(Cypher5Parser.ConstraintTypeContext ctx);

    void exitDropConstraint(Cypher5Parser.DropConstraintContext ctx);

    void exitCreateIndex(Cypher5Parser.CreateIndexContext ctx);

    void exitOldCreateIndex(Cypher5Parser.OldCreateIndexContext ctx);

    void exitCreateIndex_(Cypher5Parser.CreateIndex_Context ctx);

    void exitCreateFulltextIndex(Cypher5Parser.CreateFulltextIndexContext ctx);

    void exitFulltextNodePattern(Cypher5Parser.FulltextNodePatternContext ctx);

    void exitFulltextRelPattern(Cypher5Parser.FulltextRelPatternContext ctx);

    void exitCreateLookupIndex(Cypher5Parser.CreateLookupIndexContext ctx);

    void exitLookupIndexNodePattern(Cypher5Parser.LookupIndexNodePatternContext ctx);

    void exitLookupIndexRelPattern(Cypher5Parser.LookupIndexRelPatternContext ctx);

    void exitDropIndex(Cypher5Parser.DropIndexContext ctx);

    void exitPropertyList(Cypher5Parser.PropertyListContext ctx);

    void exitEnclosedPropertyList(Cypher5Parser.EnclosedPropertyListContext ctx);

    void exitAlterCommand(Cypher5Parser.AlterCommandContext ctx);

    void exitRenameCommand(Cypher5Parser.RenameCommandContext ctx);

    void exitGrantCommand(Cypher5Parser.GrantCommandContext ctx);

    void exitDenyCommand(Cypher5Parser.DenyCommandContext ctx);

    void exitRevokeCommand(Cypher5Parser.RevokeCommandContext ctx);

    void exitUserNames(Cypher5Parser.UserNamesContext ctx);

    void exitRoleNames(Cypher5Parser.RoleNamesContext ctx);

    void exitRoleToken(Cypher5Parser.RoleTokenContext ctx);

    void exitEnableServerCommand(Cypher5Parser.EnableServerCommandContext ctx);

    void exitAlterServer(Cypher5Parser.AlterServerContext ctx);

    void exitRenameServer(Cypher5Parser.RenameServerContext ctx);

    void exitDropServer(Cypher5Parser.DropServerContext ctx);

    void exitShowServers(Cypher5Parser.ShowServersContext ctx);

    void exitAllocationCommand(Cypher5Parser.AllocationCommandContext ctx);

    void exitDeallocateDatabaseFromServers(Cypher5Parser.DeallocateDatabaseFromServersContext ctx);

    void exitReallocateDatabases(Cypher5Parser.ReallocateDatabasesContext ctx);

    void exitCreateRole(Cypher5Parser.CreateRoleContext ctx);

    void exitDropRole(Cypher5Parser.DropRoleContext ctx);

    void exitRenameRole(Cypher5Parser.RenameRoleContext ctx);

    void exitShowRoles(Cypher5Parser.ShowRolesContext ctx);

    void exitGrantRole(Cypher5Parser.GrantRoleContext ctx);

    void exitRevokeRole(Cypher5Parser.RevokeRoleContext ctx);

    void exitCreateUser(Cypher5Parser.CreateUserContext ctx);

    void exitDropUser(Cypher5Parser.DropUserContext ctx);

    void exitRenameUser(Cypher5Parser.RenameUserContext ctx);

    void exitAlterCurrentUser(Cypher5Parser.AlterCurrentUserContext ctx);

    void exitAlterUser(Cypher5Parser.AlterUserContext ctx);

    void exitRemoveNamedProvider(Cypher5Parser.RemoveNamedProviderContext ctx);

    void exitPassword(Cypher5Parser.PasswordContext ctx);

    void exitPasswordOnly(Cypher5Parser.PasswordOnlyContext ctx);

    void exitPasswordExpression(Cypher5Parser.PasswordExpressionContext ctx);

    void exitPasswordChangeRequired(Cypher5Parser.PasswordChangeRequiredContext ctx);

    void exitUserStatus(Cypher5Parser.UserStatusContext ctx);

    void exitHomeDatabase(Cypher5Parser.HomeDatabaseContext ctx);

    void exitSetAuthClause(Cypher5Parser.SetAuthClauseContext ctx);

    void exitUserAuthAttribute(Cypher5Parser.UserAuthAttributeContext ctx);

    void exitShowUsers(Cypher5Parser.ShowUsersContext ctx);

    void exitShowCurrentUser(Cypher5Parser.ShowCurrentUserContext ctx);

    void exitShowSupportedPrivileges(Cypher5Parser.ShowSupportedPrivilegesContext ctx);

    void exitShowPrivileges(Cypher5Parser.ShowPrivilegesContext ctx);

    void exitShowRolePrivileges(Cypher5Parser.ShowRolePrivilegesContext ctx);

    void exitShowUserPrivileges(Cypher5Parser.ShowUserPrivilegesContext ctx);

    void exitPrivilegeAsCommand(Cypher5Parser.PrivilegeAsCommandContext ctx);

    void exitPrivilegeToken(Cypher5Parser.PrivilegeTokenContext ctx);

    void exitPrivilege(Cypher5Parser.PrivilegeContext ctx);

    void exitAllPrivilege(Cypher5Parser.AllPrivilegeContext ctx);

    void exitAllPrivilegeType(Cypher5Parser.AllPrivilegeTypeContext ctx);

    void exitAllPrivilegeTarget(Cypher5Parser.AllPrivilegeTargetContext ctx);

    void exitCreatePrivilege(Cypher5Parser.CreatePrivilegeContext ctx);

    void exitCreatePrivilegeForDatabase(Cypher5Parser.CreatePrivilegeForDatabaseContext ctx);

    void exitCreateNodePrivilegeToken(Cypher5Parser.CreateNodePrivilegeTokenContext ctx);

    void exitCreateRelPrivilegeToken(Cypher5Parser.CreateRelPrivilegeTokenContext ctx);

    void exitCreatePropertyPrivilegeToken(Cypher5Parser.CreatePropertyPrivilegeTokenContext ctx);

    void exitActionForDBMS(Cypher5Parser.ActionForDBMSContext ctx);

    void exitDropPrivilege(Cypher5Parser.DropPrivilegeContext ctx);

    void exitLoadPrivilege(Cypher5Parser.LoadPrivilegeContext ctx);

    void exitShowPrivilege(Cypher5Parser.ShowPrivilegeContext ctx);

    void exitSetPrivilege(Cypher5Parser.SetPrivilegeContext ctx);

    void exitPasswordToken(Cypher5Parser.PasswordTokenContext ctx);

    void exitRemovePrivilege(Cypher5Parser.RemovePrivilegeContext ctx);

    void exitWritePrivilege(Cypher5Parser.WritePrivilegeContext ctx);

    void exitDatabasePrivilege(Cypher5Parser.DatabasePrivilegeContext ctx);

    void exitDbmsPrivilege(Cypher5Parser.DbmsPrivilegeContext ctx);

    void exitDbmsPrivilegeExecute(Cypher5Parser.DbmsPrivilegeExecuteContext ctx);

    void exitAdminToken(Cypher5Parser.AdminTokenContext ctx);

    void exitProcedureToken(Cypher5Parser.ProcedureTokenContext ctx);

    void exitIndexToken(Cypher5Parser.IndexTokenContext ctx);

    void exitConstraintToken(Cypher5Parser.ConstraintTokenContext ctx);

    void exitTransactionToken(Cypher5Parser.TransactionTokenContext ctx);

    void exitUserQualifier(Cypher5Parser.UserQualifierContext ctx);

    void exitExecuteFunctionQualifier(Cypher5Parser.ExecuteFunctionQualifierContext ctx);

    void exitExecuteProcedureQualifier(Cypher5Parser.ExecuteProcedureQualifierContext ctx);

    void exitSettingQualifier(Cypher5Parser.SettingQualifierContext ctx);

    void exitGlobs(Cypher5Parser.GlobsContext ctx);

    void exitGlob(Cypher5Parser.GlobContext ctx);

    void exitGlobRecursive(Cypher5Parser.GlobRecursiveContext ctx);

    void exitGlobPart(Cypher5Parser.GlobPartContext ctx);

    void exitQualifiedGraphPrivilegesWithProperty(Cypher5Parser.QualifiedGraphPrivilegesWithPropertyContext ctx);

    void exitQualifiedGraphPrivileges(Cypher5Parser.QualifiedGraphPrivilegesContext ctx);

    void exitLabelsResource(Cypher5Parser.LabelsResourceContext ctx);

    void exitPropertiesResource(Cypher5Parser.PropertiesResourceContext ctx);

    void exitNonEmptyStringList(Cypher5Parser.NonEmptyStringListContext ctx);

    void exitGraphQualifier(Cypher5Parser.GraphQualifierContext ctx);

    void exitGraphQualifierToken(Cypher5Parser.GraphQualifierTokenContext ctx);

    void exitRelToken(Cypher5Parser.RelTokenContext ctx);

    void exitElementToken(Cypher5Parser.ElementTokenContext ctx);

    void exitNodeToken(Cypher5Parser.NodeTokenContext ctx);

    void exitDatabaseScope(Cypher5Parser.DatabaseScopeContext ctx);

    void exitGraphScope(Cypher5Parser.GraphScopeContext ctx);

    void exitCreateCompositeDatabase(Cypher5Parser.CreateCompositeDatabaseContext ctx);

    void exitCreateDatabase(Cypher5Parser.CreateDatabaseContext ctx);

    void exitPrimaryTopology(Cypher5Parser.PrimaryTopologyContext ctx);

    void exitPrimaryToken(Cypher5Parser.PrimaryTokenContext ctx);

    void exitSecondaryTopology(Cypher5Parser.SecondaryTopologyContext ctx);

    void exitSecondaryToken(Cypher5Parser.SecondaryTokenContext ctx);

    void exitDropDatabase(Cypher5Parser.DropDatabaseContext ctx);

    void exitAlterDatabase(Cypher5Parser.AlterDatabaseContext ctx);

    void exitAlterDatabaseAccess(Cypher5Parser.AlterDatabaseAccessContext ctx);

    void exitAlterDatabaseTopology(Cypher5Parser.AlterDatabaseTopologyContext ctx);

    void exitAlterDatabaseOption(Cypher5Parser.AlterDatabaseOptionContext ctx);

    void exitStartDatabase(Cypher5Parser.StartDatabaseContext ctx);

    void exitStopDatabase(Cypher5Parser.StopDatabaseContext ctx);

    void exitWaitClause(Cypher5Parser.WaitClauseContext ctx);

    void exitSecondsToken(Cypher5Parser.SecondsTokenContext ctx);

    void exitShowDatabase(Cypher5Parser.ShowDatabaseContext ctx);

    void exitCreateAlias(Cypher5Parser.CreateAliasContext ctx);

    void exitDropAlias(Cypher5Parser.DropAliasContext ctx);

    void exitAlterAlias(Cypher5Parser.AlterAliasContext ctx);

    void exitAlterAliasTarget(Cypher5Parser.AlterAliasTargetContext ctx);

    void exitAlterAliasUser(Cypher5Parser.AlterAliasUserContext ctx);

    void exitAlterAliasPassword(Cypher5Parser.AlterAliasPasswordContext ctx);

    void exitAlterAliasDriver(Cypher5Parser.AlterAliasDriverContext ctx);

    void exitAlterAliasProperties(Cypher5Parser.AlterAliasPropertiesContext ctx);

    void exitShowAliases(Cypher5Parser.ShowAliasesContext ctx);

    void exitSymbolicNameOrStringParameter(Cypher5Parser.SymbolicNameOrStringParameterContext ctx);

    void exitCommandNameExpression(Cypher5Parser.CommandNameExpressionContext ctx);

    void exitSymbolicNameOrStringParameterList(Cypher5Parser.SymbolicNameOrStringParameterListContext ctx);

    void exitSymbolicAliasNameList(Cypher5Parser.SymbolicAliasNameListContext ctx);

    void exitSymbolicAliasNameOrParameter(Cypher5Parser.SymbolicAliasNameOrParameterContext ctx);

    void exitSymbolicAliasName(Cypher5Parser.SymbolicAliasNameContext ctx);

    void exitStringListLiteral(Cypher5Parser.StringListLiteralContext ctx);

    void exitStringList(Cypher5Parser.StringListContext ctx);

    void exitStringLiteral(Cypher5Parser.StringLiteralContext ctx);

    void exitStringOrParameterExpression(Cypher5Parser.StringOrParameterExpressionContext ctx);

    void exitStringOrParameter(Cypher5Parser.StringOrParameterContext ctx);

    void exitMapOrParameter(Cypher5Parser.MapOrParameterContext ctx);

    void exitMap(Cypher5Parser.MapContext ctx);

    void exitSymbolicNameString(Cypher5Parser.SymbolicNameStringContext ctx);

    void exitEscapedSymbolicNameString(Cypher5Parser.EscapedSymbolicNameStringContext ctx);

    void exitUnescapedSymbolicNameString(Cypher5Parser.UnescapedSymbolicNameStringContext ctx);

    void exitSymbolicLabelNameString(Cypher5Parser.SymbolicLabelNameStringContext ctx);

    void exitUnescapedLabelSymbolicNameString(Cypher5Parser.UnescapedLabelSymbolicNameStringContext ctx);

    void exitEndOfFile(Cypher5Parser.EndOfFileContext ctx);
}
