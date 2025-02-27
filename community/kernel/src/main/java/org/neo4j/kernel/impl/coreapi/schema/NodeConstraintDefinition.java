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

import static java.util.Objects.requireNonNull;
import static org.neo4j.internal.helpers.collection.Iterables.single;
import static org.neo4j.kernel.impl.coreapi.schema.IndexDefinitionImpl.labelNameList;

import java.util.Arrays;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.internal.schema.ConstraintDescriptor;

abstract class NodeConstraintDefinition extends MultiPropertyConstraintDefinition {
    protected final Label label;

    NodeConstraintDefinition(
            InternalSchemaActions actions, ConstraintDescriptor constraint, Label label, String[] propertyKeys) {
        super(actions, constraint, propertyKeys);
        this.label = requireNonNull(label);
    }

    NodeConstraintDefinition(
            InternalSchemaActions actions, ConstraintDescriptor constraint, IndexDefinition indexDefinition) {
        super(actions, constraint, indexDefinition);
        if (indexDefinition.isMultiTokenIndex()) {
            throw new IllegalArgumentException(
                    "Node constraints do not support multi-token definitions. That is, they cannot apply to more than one label, "
                            + "but an attempt was made to create a node constraint on the following labels: "
                            + labelNameList(indexDefinition.getLabels(), "", "."));
        }
        this.label = single(indexDefinition.getLabels());
    }

    @Override
    public Label getLabel() {
        assertInUnterminatedTransaction();
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NodeConstraintDefinition that = (NodeConstraintDefinition) o;
        return label.name().equals(that.label.name()) && Arrays.equals(propertyKeys, that.propertyKeys);
    }

    @Override
    public int hashCode() {
        return 31 * label.name().hashCode() + Arrays.hashCode(propertyKeys);
    }
}
