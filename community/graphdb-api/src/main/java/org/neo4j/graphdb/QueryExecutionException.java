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

import org.neo4j.annotations.api.PublicApi;
import org.neo4j.gqlstatus.ErrorGqlStatusObject;
import org.neo4j.gqlstatus.GqlRuntimeException;

/**
 * This exception is thrown from the {@link Transaction#execute(String, java.util.Map) execute method}
 * when there is an error during the execution of a query.
 */
@PublicApi
public class QueryExecutionException extends GqlRuntimeException {
    private final String statusCode;

    public QueryExecutionException(
            ErrorGqlStatusObject gqlStatusObject, String message, Throwable cause, String statusCode) {
        super(gqlStatusObject, message, cause);
        this.statusCode = statusCode;
    }

    public QueryExecutionException(String message, Throwable cause, String statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /**
     * The Neo4j error <a href="https://neo4j.com/docs/developer-manual/current/reference/status-codes/">status code</a>.
     *
     * @return the Neo4j error status code.
     */
    public String getStatusCode() {
        return statusCode;
    }
}
