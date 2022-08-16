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
package org.neo4j.cypher.internal.administration

import org.neo4j.common.DependencyResolver
import org.neo4j.configuration.GraphDatabaseSettings
import org.neo4j.cypher.internal.AdministrationCommandRuntime.IdentityConverter
import org.neo4j.cypher.internal.AdministrationCommandRuntime.getNameFields
import org.neo4j.cypher.internal.AdministrationCommandRuntime.internalKey
import org.neo4j.cypher.internal.AdministrationShowCommandUtils
import org.neo4j.cypher.internal.ExecutionEngine
import org.neo4j.cypher.internal.ExecutionPlan
import org.neo4j.cypher.internal.ast.DatabaseScope
import org.neo4j.cypher.internal.ast.DefaultDatabaseScope
import org.neo4j.cypher.internal.ast.HomeDatabaseScope
import org.neo4j.cypher.internal.ast.NamedDatabaseScope
import org.neo4j.cypher.internal.ast.Return
import org.neo4j.cypher.internal.ast.ShowDatabase.ACCESS_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.ADDRESS_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.ALIASES_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.CREATION_TIME_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.CURRENT_PRIMARIES_COUNT_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.CURRENT_SECONDARIES_COUNT_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.CURRENT_STATUS_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.DATABASE_ID_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.DEFAULT_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.HOME_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.LAST_COMMITTED_TX_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.LAST_START_TIME_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.LAST_STOP_TIME_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.NAME_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.REPLICATION_LAG_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.REQUESTED_PRIMARIES_COUNT_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.REQUESTED_SECONDARIES_COUNT_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.REQUESTED_STATUS_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.ROLE_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.SERVER_ID_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.STATUS_MSG_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.STORE_COL
import org.neo4j.cypher.internal.ast.ShowDatabase.WRITER_COL
import org.neo4j.cypher.internal.ast.Yield
import org.neo4j.cypher.internal.expressions.Variable
import org.neo4j.cypher.internal.procs.SystemCommandExecutionPlan
import org.neo4j.dbms.api.DatabaseManagementService
import org.neo4j.dbms.database.DatabaseInfo
import org.neo4j.dbms.database.DatabaseInfoService
import org.neo4j.dbms.database.ExtendedDatabaseInfo
import org.neo4j.dbms.database.TopologyGraphDbmsModel.DATABASE
import org.neo4j.dbms.database.TopologyGraphDbmsModel.DATABASE_CREATED_AT_PROPERTY
import org.neo4j.dbms.database.TopologyGraphDbmsModel.DATABASE_DEFAULT_PROPERTY
import org.neo4j.dbms.database.TopologyGraphDbmsModel.DATABASE_LABEL
import org.neo4j.dbms.database.TopologyGraphDbmsModel.DATABASE_NAME
import org.neo4j.dbms.database.TopologyGraphDbmsModel.DATABASE_NAME_PROPERTY
import org.neo4j.dbms.database.TopologyGraphDbmsModel.DATABASE_PRIMARIES_PROPERTY
import org.neo4j.dbms.database.TopologyGraphDbmsModel.DATABASE_SECONDARIES_PROPERTY
import org.neo4j.dbms.database.TopologyGraphDbmsModel.DATABASE_STARTED_AT_PROPERTY
import org.neo4j.dbms.database.TopologyGraphDbmsModel.DATABASE_STATUS_PROPERTY
import org.neo4j.dbms.database.TopologyGraphDbmsModel.DATABASE_STOPPED_AT_PROPERTY
import org.neo4j.dbms.database.TopologyGraphDbmsModel.NAME_PROPERTY
import org.neo4j.dbms.database.TopologyGraphDbmsModel.TARGETS
import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.Label
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.RelationshipType.withName
import org.neo4j.graphdb.Transaction
import org.neo4j.internal.kernel.api.security.AdminActionOnResource
import org.neo4j.internal.kernel.api.security.PrivilegeAction
import org.neo4j.internal.kernel.api.security.SecurityAuthorizationHandler
import org.neo4j.internal.kernel.api.security.SecurityContext
import org.neo4j.internal.kernel.api.security.Segment
import org.neo4j.kernel.database.DefaultDatabaseResolver
import org.neo4j.kernel.database.NormalizedDatabaseName
import org.neo4j.kernel.internal.GraphDatabaseAPI
import org.neo4j.storageengine.api.StoreIdProvider
import org.neo4j.storageengine.util.StoreIdDecodeUtils
import org.neo4j.values.AnyValue
import org.neo4j.values.storable.Values
import org.neo4j.values.virtual.MapValue
import org.neo4j.values.virtual.VirtualValues

import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.jdk.CollectionConverters.SetHasAsJava
import scala.jdk.OptionConverters.RichOptional
import scala.util.Try
import scala.util.Using

case class ShowDatabasesExecutionPlanner(
  resolver: DependencyResolver,
  defaultDatabaseResolver: DefaultDatabaseResolver,
  normalExecutionEngine: ExecutionEngine,
  securityAuthorizationHandler: SecurityAuthorizationHandler
)(implicit extendedDatabaseInfoMapper: DatabaseInfoMapper[ExtendedDatabaseInfo]) {

  private val accessibleDbsKey = internalKey("accessibleDbs")
  private val dbms = resolver.resolveDependency(classOf[DatabaseManagementService])
  private val infoService = resolver.resolveDependency(classOf[DatabaseInfoService])

  def planShowDatabases(
    scope: DatabaseScope,
    verbose: Boolean,
    symbols: List[String],
    yields: Option[Yield],
    returns: Option[Return]
  ): ExecutionPlan = {
    val usernameKey = internalKey("username")
    val paramGenerator: (Transaction, SecurityContext) => MapValue =
      (tx, securityContext) => generateShowAccessibleDatabasesParameter(tx, securityContext, yields, verbose)

    val (extraFilter, params, paramConverter) = scope match {
      // show default database
      case _: DefaultDatabaseScope => (s"WHERE default = true", VirtualValues.EMPTY_MAP, IdentityConverter)
      // show home database
      case _: HomeDatabaseScope => (s"WHERE home = true", VirtualValues.EMPTY_MAP, IdentityConverter)
      // show database name
      case NamedDatabaseScope(p) =>
        val nameFields = getNameFields("databaseName", p, valueMapper = s => new NormalizedDatabaseName(s).name())
        (
          s"WHERE $$`${nameFields.nameKey}` IN aliases",
          VirtualValues.map(Array(nameFields.nameKey), Array(nameFields.nameValue)),
          nameFields.nameConverter
        )
      // show all databases
      case _ => ("", VirtualValues.EMPTY_MAP, IdentityConverter)
    }

    val verboseColumns =
      if (verbose) {
        s""", props.$DATABASE_ID_COL as $DATABASE_ID_COL,
           |props.$SERVER_ID_COL as $SERVER_ID_COL,
           |props.$CURRENT_PRIMARIES_COUNT_COL as $CURRENT_PRIMARIES_COUNT_COL,
           |props.$CURRENT_SECONDARIES_COUNT_COL as $CURRENT_SECONDARIES_COUNT_COL,
           |d.$DATABASE_PRIMARIES_PROPERTY as $REQUESTED_PRIMARIES_COUNT_COL,
           |d.$DATABASE_SECONDARIES_PROPERTY as $REQUESTED_SECONDARIES_COUNT_COL,
           |props.$LAST_COMMITTED_TX_COL as $LAST_COMMITTED_TX_COL,
           |props.$REPLICATION_LAG_COL as $REPLICATION_LAG_COL,
           |d.$DATABASE_CREATED_AT_PROPERTY as $CREATION_TIME_COL,
           |d.$DATABASE_STARTED_AT_PROPERTY as $LAST_START_TIME_COL,
           |d.$DATABASE_STOPPED_AT_PROPERTY as $LAST_STOP_TIME_COL,
           |props.$STORE_COL as $STORE_COL
           |""".stripMargin
      } else {
        ""
      }
    val verboseNames =
      if (verbose) {
        s", $DATABASE_ID_COL, $SERVER_ID_COL, $REQUESTED_PRIMARIES_COUNT_COL, $REQUESTED_SECONDARIES_COUNT_COL, $CURRENT_PRIMARIES_COUNT_COL, " +
          s"$CURRENT_SECONDARIES_COUNT_COL, $CREATION_TIME_COL, $LAST_START_TIME_COL, $LAST_STOP_TIME_COL, $STORE_COL, $LAST_COMMITTED_TX_COL, $REPLICATION_LAG_COL"
      } else {
        ""
      }
    val returnClause = AdministrationShowCommandUtils.generateReturnClause(symbols, yields, returns, Seq("name"))

    val query = Predef.augmentString(
      s"""// First resolve which database is the home database
         |OPTIONAL MATCH (default:$DATABASE_LABEL {$DATABASE_DEFAULT_PROPERTY: true})
         |OPTIONAL MATCH (user:User {$DATABASE_NAME_PROPERTY: $$`$usernameKey`})
         |WITH coalesce(user.homeDatabase, default.$DATABASE_NAME_PROPERTY) as homeDbName
         |
         |UNWIND $$`$accessibleDbsKey` AS props
         |CALL {
         |    WITH props
         |    MATCH (d:$DATABASE)<-[:$TARGETS]-(:$DATABASE_NAME {$NAME_PROPERTY: props.name}) RETURN d
         |  UNION
         |    WITH props
         |    MATCH (d:$DATABASE {$NAME_PROPERTY: props.name}) RETURN d
         |}
         |WITH d, props, homeDbName
         |OPTIONAL MATCH (d)<-[:$TARGETS]-(a:$DATABASE_NAME)
         |WITH d, props, homeDbName, a.name as aliasName ORDER BY aliasName
         |WITH d.name as name,
         |collect(aliasName) + [d.name] as aliases,
         |props.$ACCESS_COL as $ACCESS_COL,
         |props.$ADDRESS_COL as $ADDRESS_COL,
         |props.$ROLE_COL as $ROLE_COL,
         |props.$WRITER_COL as $WRITER_COL,
         |d.$DATABASE_STATUS_PROPERTY as requestedStatus,
         |props.$CURRENT_STATUS_COL as $CURRENT_STATUS_COL,
         |props.$STATUS_MSG_COL as $STATUS_MSG_COL,
         |d.$DATABASE_DEFAULT_PROPERTY as default,
         |homeDbName,
         |coalesce( homeDbName in collect(aliasName) + [d.name], false ) as home
         |$verboseColumns
         |$extraFilter
         |WITH name AS $NAME_COL,
         |[alias in aliases where name <> alias] AS $ALIASES_COL,
         |$ACCESS_COL,
         |$ADDRESS_COL,
         |$ROLE_COL,
         |$WRITER_COL,
         |requestedStatus AS $REQUESTED_STATUS_COL,
         |$CURRENT_STATUS_COL,
         |$STATUS_MSG_COL,
         |default AS $DEFAULT_COL,
         |home AS $HOME_COL
         |$verboseNames
         |$returnClause"""
    ).stripMargin
    SystemCommandExecutionPlan(
      scope.showCommandName,
      normalExecutionEngine,
      securityAuthorizationHandler,
      query,
      params,
      parameterGenerator = paramGenerator,
      parameterConverter = paramConverter
    )
  }

  private def generateShowAccessibleDatabasesParameter(
    transaction: Transaction,
    securityContext: SecurityContext,
    maybeYield: Option[Yield],
    verbose: Boolean
  ): MapValue = {
    def accessForDatabase(database: Node, roles: java.util.Set[String]): Option[Boolean] = {
      // (:Role)-[p]->(:Privilege {action: 'access'})-[s:SCOPE]->()-[f:FOR]->(d:Database)
      var result: Seq[Boolean] = Seq.empty
      val dbRelationships = database.getRelationships(Direction.INCOMING, withName("FOR"))
      try {
        dbRelationships.forEach { f =>
          val startRelationships = f.getStartNode.getRelationships(Direction.INCOMING, withName("SCOPE"))
          try {
            startRelationships.forEach { s =>
              val privilegeNode = s.getStartNode
              if (privilegeNode.getProperty("action").equals("access")) {
                val prevRelationships = privilegeNode.getRelationships(Direction.INCOMING)
                try {
                  prevRelationships.forEach { p =>
                    val roleName = p.getStartNode.getProperty("name")
                    if (roles.contains(roleName)) {
                      p.getType.name() match {
                        case "DENIED"  => result = result :+ false
                        case "GRANTED" => result = result :+ true
                        case _         =>
                      }
                    }
                  }
                } finally {
                  prevRelationships.close()
                }
              }
            }
          } finally {
            startRelationships.close()
          }
        }
      } finally {
        dbRelationships.close()
      }
      result.reduceOption(_ && _)
    }

    val allowsDatabaseManagement: Boolean =
      securityContext.allowsAdminAction(new AdminActionOnResource(
        PrivilegeAction.CREATE_DATABASE,
        AdminActionOnResource.DatabaseScope.ALL,
        Segment.ALL
      )).allowsAccess() ||
        securityContext.allowsAdminAction(new AdminActionOnResource(
          PrivilegeAction.DROP_DATABASE,
          AdminActionOnResource.DatabaseScope.ALL,
          Segment.ALL
        )).allowsAccess() ||
        securityContext.allowsAdminAction(new AdminActionOnResource(
          PrivilegeAction.ALTER_DATABASE,
          AdminActionOnResource.DatabaseScope.ALL,
          Segment.ALL
        )).allowsAccess() ||
        securityContext.allowsAdminAction(new AdminActionOnResource(
          PrivilegeAction.SET_DATABASE_ACCESS,
          AdminActionOnResource.DatabaseScope.ALL,
          Segment.ALL
        )).allowsAccess()
    val roles = securityContext.mode().roles()

    def databaseAccess(label: String) =
      Using(transaction.findNodes(Label.label(label))) {
        defaultDatabaseNodes =>
          defaultDatabaseNodes.asScala.flatMap(defaultDatabaseNode =>
            accessForDatabase(defaultDatabaseNode, roles)
          ).reduceOption(_ && _)
      }.get
    val allDatabaseAccess = databaseAccess("DatabaseAll")
    val defaultDatabaseAccess = databaseAccess("DatabaseDefault")
    val defaultDatabaseName = defaultDatabaseResolver.defaultDatabase(securityContext.subject().executingUser())

    val accessibleDatabases =
      transaction.findNodes(DATABASE_LABEL).asScala.foldLeft[Seq[String]](Seq.empty) { (acc, dbNode) =>
        val dbName = dbNode.getProperty(DATABASE_NAME_PROPERTY).toString
        val isDefault = dbName.equals(defaultDatabaseName)
        if (dbName.equals(GraphDatabaseSettings.SYSTEM_DATABASE_NAME)) {
          acc :+ dbName
        } else if (allowsDatabaseManagement) {
          acc :+ dbName
        } else {
          (accessForDatabase(dbNode, roles), allDatabaseAccess, defaultDatabaseAccess, isDefault) match {
            // denied
            case (Some(false), _, _, _)    => acc
            case (_, Some(false), _, _)    => acc
            case (_, _, Some(false), true) => acc

            // granted
            case (Some(true), _, _, _)    => acc :+ dbName
            case (_, Some(true), _, _)    => acc :+ dbName
            case (_, _, Some(true), true) => acc :+ dbName

            // no privilege
            case _ => acc
          }
        }
      }

    val username = Option(securityContext.subject().executingUser()) match {
      case None       => Values.NO_VALUE
      case Some("")   => Values.NO_VALUE
      case Some(user) => Values.stringValue(user)
    }

    val dbMetadata =
      if (verbose && maybeYield.isDefined && requiresDetailedLookup(maybeYield.get)) {
        requestDetailedInfo(accessibleDatabases).asJava
      } else {
        lookupCachedInfo(accessibleDatabases).asJava
      }
    VirtualValues.map(
      Array(accessibleDbsKey, internalKey("username")),
      Array(VirtualValues.fromList(dbMetadata), username)
    )
  }

  private val detailedLookupCols = Set(
    STORE_COL,
    LAST_COMMITTED_TX_COL,
    REPLICATION_LAG_COL,
    CURRENT_PRIMARIES_COUNT_COL,
    CURRENT_SECONDARIES_COUNT_COL
  )

  private def requiresDetailedLookup(yields: Yield): Boolean = {
    yields.returnItems.includeExisting || yields.returnItems.items.map(_.expression).exists {
      case Variable(name) => detailedLookupCols.contains(name)
      case _              => false
    }
  }

  private def lookupCachedInfo(databaseNames: Seq[String]): List[AnyValue] = {
    val dbInfos = infoService.lookupCachedInfo(databaseNames.toSet.asJava).asScala
    dbInfos.map(info => BaseDatabaseInfoMapper.toMapValue(dbms, info)).toList
  }

  private def requestDetailedInfo(databaseNames: Seq[String])(implicit
  mapper: DatabaseInfoMapper[ExtendedDatabaseInfo]): List[AnyValue] = {
    val dbInfos = infoService.requestDetailedInfo(databaseNames.toSet.asJava).asScala
    dbInfos.map(info => mapper.toMapValue(dbms, info)).toList
  }
}

trait DatabaseInfoMapper[T <: DatabaseInfo] {
  def toMapValue(databaseManagementService: DatabaseManagementService, extendedDatabaseInfo: T): MapValue
}

object BaseDatabaseInfoMapper extends DatabaseInfoMapper[DatabaseInfo] {

  override def toMapValue(
    databaseManagementService: DatabaseManagementService,
    extendedDatabaseInfo: DatabaseInfo
  ): MapValue = VirtualValues.map(
    Array(
      NAME_COL,
      ACCESS_COL,
      ADDRESS_COL,
      ROLE_COL,
      WRITER_COL,
      CURRENT_STATUS_COL,
      STATUS_MSG_COL,
      DATABASE_ID_COL,
      SERVER_ID_COL
    ),
    Array(
      Values.stringValue(extendedDatabaseInfo.namedDatabaseId().name()),
      Values.stringValue(extendedDatabaseInfo.access().getStringRepr),
      extendedDatabaseInfo.boltAddress().map[AnyValue](s => Values.stringValue(s.toString)).orElse(Values.NO_VALUE),
      Values.stringValue(extendedDatabaseInfo.role()),
      Values.booleanValue(extendedDatabaseInfo.writer()),
      Values.stringValue(extendedDatabaseInfo.status()),
      Values.stringValue(extendedDatabaseInfo.statusMessage()),
      getDatabaseId(databaseManagementService, extendedDatabaseInfo.namedDatabaseId().name()).map(
        Values.stringValue
      ).getOrElse(Values.NO_VALUE),
      extendedDatabaseInfo.serverId().toScala.map(srvId => Values.stringValue(srvId.uuid().toString)).getOrElse(
        Values.NO_VALUE
      )
    )
  )

  private def getDatabaseId(dbms: DatabaseManagementService, dbName: String): Option[String] = {
    Try(dbms.database(dbName).asInstanceOf[GraphDatabaseAPI]).toOption.flatMap(graphDatabaseAPI =>
      if (graphDatabaseAPI.isAvailable(0)) {
        val storeIdProvider = graphDatabaseAPI.getDependencyResolver.resolveDependency(classOf[StoreIdProvider])
        Some(StoreIdDecodeUtils.decodeId(storeIdProvider))
      } else {
        None
      }
    )
  }
}

object CommunityExtendedDatabaseInfoMapper extends DatabaseInfoMapper[ExtendedDatabaseInfo] {

  override def toMapValue(
    databaseManagementService: DatabaseManagementService,
    extendedDatabaseInfo: ExtendedDatabaseInfo
  ): MapValue =
    BaseDatabaseInfoMapper.toMapValue(databaseManagementService, extendedDatabaseInfo).updatedWith(
      VirtualValues.map(
        Array(
          CURRENT_PRIMARIES_COUNT_COL,
          CURRENT_SECONDARIES_COUNT_COL,
          STORE_COL,
          LAST_COMMITTED_TX_COL,
          REPLICATION_LAG_COL
        ),
        Array(
          Values.longValue(extendedDatabaseInfo.primariesCount()),
          Values.longValue(extendedDatabaseInfo.secondariesCount()),
          extendedDatabaseInfo.storeId().toScala.map(Values.stringValue).getOrElse(Values.NO_VALUE),
          Values.NO_VALUE,
          Values.longValue(0)
        )
      )
    )
}
