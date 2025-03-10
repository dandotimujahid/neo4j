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
package org.neo4j.fabric.stream.summary;

import java.util.Collection;
import org.neo4j.graphdb.ExecutionPlanDescription;
import org.neo4j.graphdb.GqlStatusObject;
import org.neo4j.graphdb.Notification;
import org.neo4j.graphdb.QueryStatistics;

public interface Summary {
    /**
     * @return The plan description of the query.
     */
    ExecutionPlanDescription executionPlanDescription();

    /**
     * @return all notifications and warnings of the query.
     */
    Collection<Notification> getNotifications();

    /**
     * @return all GQL-status objects of the query.
     */
    Collection<GqlStatusObject> getGqlStatusObjects();

    QueryStatistics getQueryStatistics();
}
