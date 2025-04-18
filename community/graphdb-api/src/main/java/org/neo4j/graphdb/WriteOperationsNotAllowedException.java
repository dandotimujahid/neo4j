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
package org.neo4j.graphdb;

import static java.lang.String.format;

import org.neo4j.gqlstatus.ErrorGqlStatusObject;
import org.neo4j.gqlstatus.ErrorGqlStatusObjectImplementation;
import org.neo4j.gqlstatus.GqlRuntimeException;
import org.neo4j.gqlstatus.GqlStatusInfoCodes;
import org.neo4j.kernel.api.exceptions.Status;

public class WriteOperationsNotAllowedException extends GqlRuntimeException implements Status.HasStatus {
    private final Status statusCode;

    public static final String NOT_LEADER_ERROR_MSG =
            "No write operations are allowed directly on this database. Writes must pass through the leader. "
                    + "The role of this server is: %s";

    @Deprecated
    public WriteOperationsNotAllowedException(String message, Status statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public WriteOperationsNotAllowedException(ErrorGqlStatusObject gqlStatusObject, String message, Status statusCode) {
        super(gqlStatusObject, message);
        this.statusCode = statusCode;
    }

    public static WriteOperationsNotAllowedException notALeader(String currentRole) {
        var gql = ErrorGqlStatusObjectImplementation.from(GqlStatusInfoCodes.STATUS_08N07)
                .build();
        return new WriteOperationsNotAllowedException(
                gql, format(NOT_LEADER_ERROR_MSG, currentRole), Status.Cluster.NotALeader);
    }

    /** The Neo4j status code associated with this exception type. */
    @Override
    public Status status() {
        return statusCode;
    }
}
