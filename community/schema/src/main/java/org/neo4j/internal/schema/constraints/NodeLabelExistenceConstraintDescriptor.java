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
package org.neo4j.internal.schema.constraints;

import org.neo4j.internal.schema.ConstraintDescriptor;

public interface NodeLabelExistenceConstraintDescriptor extends ConstraintDescriptor {

    /**
     * For a constraint that enforces that nodes with label `:A`
     * should also have the label `:B`.
     * @return the token id for label `:B`.
     */
    int requiredLabelId();

    @Override
    NodeLabelExistenceConstraintDescriptor withName(String name);

    @Override
    NodeLabelExistenceConstraintDescriptor withId(long id);
}
