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
import org.neo4j.gqlstatus.CommonGqlStatusObject;
import org.neo4j.gqlstatus.NotificationClassification;

/**
 * Representation for the GQL-status object from the execution of a successful query.
 */
@PublicApi
public interface GqlStatusObject extends CommonGqlStatusObject {
    /**
     * Returns the severity level of this GQL-status object. This will be one of [WARNING, INFORMATION]
     * @return the severity level of the GQL-status object.
     */
    default SeverityLevel getSeverity() {
        return SeverityLevel.UNKNOWN;
    }

    /**
     * The position in the query which this GQL-status object is associated with.
     * Not all GQL-status objects have a unique position which they are associated with and
     * should in that case return {@link InputPosition#empty}
     *
     * @return the position in the query which the GQL-status code is referring to, or
     * {@link InputPosition#empty} if no position is associated with this GQL-status code.
     */
    default InputPosition getPosition() {
        return InputPosition.empty;
    }

    /**
     * Returns the classification of this GQL-status object.
     * @return the classification of the GQL-status object.
     */
    default NotificationClassification getClassification() {
        return NotificationClassification.UNKNOWN;
    }
}
