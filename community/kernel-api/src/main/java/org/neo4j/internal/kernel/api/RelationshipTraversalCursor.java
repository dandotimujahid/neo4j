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
package org.neo4j.internal.kernel.api;

/**
 * Cursor for traversing the relationships of a single node.
 */
public interface RelationshipTraversalCursor extends RelationshipTraversalEntities, RelationshipDataAccessor, Cursor {
    /**
     * Get the other node, the one that this cursor was not initialized from.
     * <p>
     * Relationship cursors have context, and know which node they are traversing relationships for, making it
     * possible and convenient to access the other node.
     *
     * @param cursor the cursor to use for accessing the other node.
     */
    void otherNode(NodeCursor cursor);
}
