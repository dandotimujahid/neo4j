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
package org.neo4j.kernel.impl.index.schema;

import org.neo4j.internal.kernel.api.IndexQueryConstraints;
import org.neo4j.internal.kernel.api.PropertyIndexQuery;
import org.neo4j.internal.schema.IndexDescriptor;
import org.neo4j.kernel.api.index.IndexProgressor;
import org.neo4j.values.storable.Value;

/**
 * Simple NodeValueClient test utility.
 */
public class GatheringNodeValueClient implements IndexProgressor.EntityValueClient {
    public long reference;
    public Value[] values;
    public IndexDescriptor descriptor;
    public IndexProgressor progressor;
    public PropertyIndexQuery[] query;
    public IndexQueryConstraints constraints;
    public boolean needStoreFilter;

    @Override
    public void initializeQuery(
            IndexDescriptor descriptor,
            IndexProgressor progressor,
            boolean indexIncludesTransactionState,
            boolean needStoreFilter,
            IndexQueryConstraints constraints,
            PropertyIndexQuery... query) {
        this.descriptor = descriptor;
        this.progressor = progressor;
        this.query = query;
        this.constraints = constraints;
        this.needStoreFilter = needStoreFilter;
    }

    @Override
    public boolean acceptEntity(long reference, float score, Value... values) {
        this.reference = reference;
        this.values = values;
        return true;
    }

    @Override
    public boolean needsValues() {
        return constraints.needsValues();
    }
}
