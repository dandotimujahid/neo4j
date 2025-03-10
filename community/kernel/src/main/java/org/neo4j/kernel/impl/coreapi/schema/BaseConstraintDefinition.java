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

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.ConstraintType;
import org.neo4j.graphdb.schema.PropertyType;
import org.neo4j.internal.schema.ConstraintDescriptor;

public abstract class BaseConstraintDefinition implements ConstraintDefinition {
    protected final InternalSchemaActions actions;
    protected ConstraintDescriptor constraint;

    BaseConstraintDefinition(InternalSchemaActions actions, ConstraintDescriptor constraint) {
        this.actions = requireNonNull(actions);
        this.constraint = requireNonNull(constraint);
    }

    public ConstraintDescriptor getConstraintReference() {
        return constraint;
    }

    @Override
    public Label getLabel() {
        assertInUnterminatedTransaction();
        throw new IllegalStateException("Constraint is associated with relationships");
    }

    @Override
    public RelationshipType getRelationshipType() {
        assertInUnterminatedTransaction();
        throw new IllegalStateException("Constraint is associated with nodes");
    }

    @Override
    public Iterable<String> getPropertyKeys() {
        assertInUnterminatedTransaction();
        throw new IllegalStateException("Constraint is not associated with properties");
    }

    @Override
    public boolean isConstraintType(ConstraintType type) {
        assertInUnterminatedTransaction();
        return getConstraintType().equals(type);
    }

    @Override
    public String getName() {
        return constraint.getName();
    }

    @Override
    public void drop() {
        assertInUnterminatedTransaction();
        actions.dropConstraint(constraint);
    }

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    /**
     * Returned string is used in shell's constraint listing.
     */
    @Override
    public abstract String toString();

    protected void assertInUnterminatedTransaction() {
        actions.assertInOpenTransaction();
    }

    @Override
    public PropertyType[] getPropertyType() {
        assertInUnterminatedTransaction();
        throw new IllegalStateException("Constraint is not a property type constraint.");
    }
}
