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
package org.neo4j.internal.kernel.api.helpers.traversal.ppbfs;

import org.neo4j.internal.kernel.api.RelationshipTraversalEntities;
import org.neo4j.values.virtual.VirtualRelationshipValue;

public interface MREValidator {
    boolean validateRelationships(
            TraversalDirection direction,
            int depth,
            VirtualRelationshipValue[] rels,
            RelationshipTraversalEntities relCursor);

    MREValidator TRAIL_MODE = (direction, depth, rels, relCursor) -> {
        boolean isUnique = true;
        switch (direction) {
            case FORWARD -> {
                for (int i = 0; i < depth && isUnique; i++) {
                    isUnique = rels[i].id() != relCursor.relationshipReference();
                }
            }
            case BACKWARD -> {
                for (int i = rels.length - 1; i > rels.length - depth - 1 && isUnique; i--) {
                    isUnique = rels[i].id() != relCursor.relationshipReference();
                }
            }
        }
        return isUnique;
    };

    MREValidator WALK_MODE = (direction, depth, rels, relCursor) -> true;
}
