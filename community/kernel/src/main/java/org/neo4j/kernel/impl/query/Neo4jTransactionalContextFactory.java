/*
 * Copyright (c) 2002-2019 "Neo4j,"
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
package org.neo4j.kernel.impl.query;

import java.util.function.Supplier;

import org.neo4j.common.DependencyResolver;
import org.neo4j.kernel.GraphDatabaseQueryService;
import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.query.ExecutingQuery;
import org.neo4j.kernel.impl.core.ThreadToStatementContextBridge;
import org.neo4j.kernel.impl.coreapi.InternalTransaction;
import org.neo4j.kernel.impl.factory.GraphDatabaseFacade;
import org.neo4j.values.virtual.MapValue;

import static org.neo4j.function.Suppliers.lazySingleton;

public class Neo4jTransactionalContextFactory implements TransactionalContextFactory
{
    private final Supplier<Statement> statementSupplier;
    private final Neo4jTransactionalContext.Creator contextCreator;

    public static TransactionalContextFactory create(
        GraphDatabaseFacade.SPI spi,
        ThreadToStatementContextBridge txBridge )
    {
        Supplier<GraphDatabaseQueryService> queryService = lazySingleton( spi::queryService );
        Neo4jTransactionalContext.Creator contextCreator = ( tx, initialStatement, executingQuery ) ->
                new Neo4jTransactionalContext( queryService.get(), txBridge, tx, initialStatement, executingQuery );

        return new Neo4jTransactionalContextFactory( txBridge, contextCreator );
    }

    @Deprecated
    public static TransactionalContextFactory create( GraphDatabaseQueryService queryService )
    {
        DependencyResolver resolver = queryService.getDependencyResolver();
        ThreadToStatementContextBridge txBridge = resolver.resolveDependency( ThreadToStatementContextBridge.class );
        Neo4jTransactionalContext.Creator contextCreator =
                ( tx, initialStatement, executingQuery ) ->
                        new Neo4jTransactionalContext(
                                queryService,
                                txBridge,
                                tx,
                                initialStatement,
                                executingQuery
                        );

        return new Neo4jTransactionalContextFactory( txBridge, contextCreator );
    }

    // Please use the factory methods above to actually construct an instance
    private Neo4jTransactionalContextFactory(
        Supplier<Statement> statementSupplier,
        Neo4jTransactionalContext.Creator contextCreator )
    {
        this.statementSupplier = statementSupplier;
        this.contextCreator = contextCreator;
    }

    @Override
    public final Neo4jTransactionalContext newContext(
        InternalTransaction tx,
        String queryText,
        MapValue queryParameters
    )
    {
        Statement initialStatement = statementSupplier.get();
        ExecutingQuery executingQuery = initialStatement.queryRegistration().startQueryExecution( queryText, queryParameters );
        return contextCreator.create( tx, initialStatement, executingQuery );
    }
}
