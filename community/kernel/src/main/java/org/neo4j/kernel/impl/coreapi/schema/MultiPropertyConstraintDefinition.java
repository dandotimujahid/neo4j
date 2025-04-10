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
package org.neo4j.kernel.impl.coreapi.schema;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static org.neo4j.internal.helpers.NameUtil.escapeName;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.internal.helpers.collection.Iterables;
import org.neo4j.internal.schema.ConstraintDescriptor;

abstract class MultiPropertyConstraintDefinition extends BaseConstraintDefinition {
    protected final String[] propertyKeys;

    MultiPropertyConstraintDefinition(
            InternalSchemaActions actions, ConstraintDescriptor constraint, String[] propertyKeys) {
        super(actions, constraint);
        this.propertyKeys = requireNonEmpty(propertyKeys);
    }

    MultiPropertyConstraintDefinition(
            InternalSchemaActions actions, ConstraintDescriptor constraint, IndexDefinition indexDefinition) {
        super(actions, constraint);
        this.propertyKeys = requireNonEmpty(Iterables.asArray(String.class, indexDefinition.getPropertyKeys()));
    }

    private static String[] requireNonEmpty(String[] array) {
        requireNonNull(array);
        if (array.length < 1) {
            throw new IllegalArgumentException("Property constraint must have at least one property");
        }
        for (String field : array) {
            if (field == null) {
                throw new IllegalArgumentException("Property constraints cannot have null property names");
            }
        }
        return array;
    }

    @Override
    public Iterable<String> getPropertyKeys() {
        assertInUnterminatedTransaction();
        return asList(propertyKeys);
    }

    String propertyText(String entityVariable) {
        if (propertyKeys.length == 1) {
            return entityVariable + "." + escapeName(propertyKeys[0]);
        } else {
            return "("
                    + Arrays.stream(propertyKeys)
                            .map(p -> entityVariable + "." + escapeName(p))
                            .collect(Collectors.joining(","))
                    + ")";
        }
    }
}
