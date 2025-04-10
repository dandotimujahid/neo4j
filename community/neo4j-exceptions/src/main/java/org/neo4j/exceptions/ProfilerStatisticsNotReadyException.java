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
package org.neo4j.exceptions;

import org.neo4j.gqlstatus.ErrorGqlStatusObject;
import org.neo4j.gqlstatus.ErrorGqlStatusObjectImplementation;
import org.neo4j.gqlstatus.GqlStatusInfoCodes;
import org.neo4j.kernel.api.exceptions.Status;

public class ProfilerStatisticsNotReadyException extends Neo4jException {
    private static final String ERROR_MSG =
            "This result has not been materialised yet. Iterate over it to get profiler stats.";

    private ProfilerStatisticsNotReadyException(ErrorGqlStatusObject gqlStatusObject) {
        super(gqlStatusObject, ERROR_MSG);
    }

    public static ProfilerStatisticsNotReadyException invalidUseOfProfile() {
        var gql = ErrorGqlStatusObjectImplementation.from(GqlStatusInfoCodes.STATUS_22000)
                .withCause(ErrorGqlStatusObjectImplementation.from(GqlStatusInfoCodes.STATUS_22N53)
                        .build())
                .build();
        return new ProfilerStatisticsNotReadyException(gql);
    }

    @Override
    public Status status() {
        return Status.Statement.ExecutionFailed;
    }
}
