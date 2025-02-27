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
package org.neo4j.kernel.impl.index.schema.tracking;

import java.io.IOException;
import java.nio.file.OpenOption;
import org.eclipse.collections.api.set.ImmutableSet;
import org.neo4j.common.TokenNameLookup;
import org.neo4j.internal.schema.IndexDescriptor;
import org.neo4j.internal.schema.StorageEngineIndexingBehaviour;
import org.neo4j.kernel.api.index.IndexAccessor;
import org.neo4j.kernel.api.index.IndexProvider;
import org.neo4j.kernel.impl.api.index.IndexSamplingConfig;
import org.neo4j.values.ElementIdMapper;

public class TrackingReadersIndexProvider extends IndexProvider.Delegating {
    private final IndexProvider indexProvider;

    TrackingReadersIndexProvider(IndexProvider copySource) {
        super(copySource);
        this.indexProvider = copySource;
    }

    @Override
    public IndexAccessor getOnlineAccessor(
            IndexDescriptor descriptor,
            IndexSamplingConfig samplingConfig,
            TokenNameLookup tokenNameLookup,
            ElementIdMapper elementIdMapper,
            ImmutableSet<OpenOption> openOptions,
            boolean readOnly,
            StorageEngineIndexingBehaviour indexingBehaviour)
            throws IOException {
        return new TrackingReadersIndexAccessor(indexProvider.getOnlineAccessor(
                descriptor,
                samplingConfig,
                tokenNameLookup,
                elementIdMapper,
                openOptions,
                readOnly,
                indexingBehaviour));
    }
}
