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
package org.neo4j.procedure.builtin.routing;

import org.neo4j.dbms.routing.RoutingService;
import org.neo4j.internal.kernel.api.exceptions.ProcedureException;
import org.neo4j.kernel.api.procedure.GlobalProcedures;
import org.neo4j.logging.InternalLogProvider;

public class RoutingProcedureInstaller {
    public static void install(
            GlobalProcedures globalProcedures, RoutingService routingService, InternalLogProvider logProvider)
            throws ProcedureException {
        var getRoutingTableProcedures = GetRoutingTableProcedure.from(routingService, logProvider);
        globalProcedures.register(getRoutingTableProcedures.old());
        globalProcedures.register(getRoutingTableProcedures.current());
    }
}
