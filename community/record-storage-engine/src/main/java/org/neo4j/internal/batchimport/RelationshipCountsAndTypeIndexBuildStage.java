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
package org.neo4j.internal.batchimport;

import static org.neo4j.internal.batchimport.RecordIdIterators.allIn;

import java.util.function.Function;
import org.neo4j.batchimport.api.Configuration;
import org.neo4j.batchimport.api.IndexImporterFactory;
import org.neo4j.counts.CountsUpdater;
import org.neo4j.internal.batchimport.cache.NodeLabelsCache;
import org.neo4j.internal.batchimport.cache.NumberArrayFactory;
import org.neo4j.internal.batchimport.staging.BatchFeedStep;
import org.neo4j.internal.batchimport.staging.ReadRecordsStep;
import org.neo4j.internal.batchimport.staging.Stage;
import org.neo4j.internal.batchimport.staging.Step;
import org.neo4j.internal.batchimport.store.BatchingNeoStores;
import org.neo4j.internal.helpers.progress.ProgressListener;
import org.neo4j.io.pagecache.context.CursorContext;
import org.neo4j.io.pagecache.context.CursorContextFactory;
import org.neo4j.io.pagecache.tracing.PageCacheTracer;
import org.neo4j.kernel.impl.store.RelationshipStore;
import org.neo4j.memory.MemoryTracker;
import org.neo4j.storageengine.api.cursor.StoreCursors;

/**
 * Reads all records from {@link RelationshipStore} and process the counts in them. Uses a {@link NodeLabelsCache} previously populated by f.ex {@link
 * NodeCountsStage}.
 */
public class RelationshipCountsAndTypeIndexBuildStage extends Stage {
    public static final String NAME = "Relationship counts and relationship type index build";

    public RelationshipCountsAndTypeIndexBuildStage(
            Configuration config,
            BatchingNeoStores neoStores,
            NodeLabelsCache cache,
            RelationshipStore relationshipStore,
            int highLabelId,
            int highRelationshipTypeId,
            CountsUpdater countsUpdater,
            NumberArrayFactory cacheFactory,
            ProgressListener progressListener,
            IndexImporterFactory indexImporterFactory,
            CursorContextFactory contextFactory,
            PageCacheTracer pageCacheTracer,
            Function<CursorContext, StoreCursors> storeCursorsCreator,
            MemoryTracker memoryTracker) {
        super(NAME, null, config, Step.RECYCLE_BATCHES);
        add(new BatchFeedStep(control(), config, allIn(relationshipStore, config), relationshipStore.getRecordSize()));
        add(new ReadRecordsStep<>(control(), config, false, relationshipStore, contextFactory));
        if (config.indexConfig().createRelationshipIndex()) {
            add(new RelationshipTypeIndexWriterStep(
                    control(),
                    config,
                    neoStores,
                    indexImporterFactory,
                    memoryTracker,
                    contextFactory,
                    pageCacheTracer,
                    storeCursorsCreator));
        }
        add(new ProcessRelationshipCountsDataStep(
                control(),
                cache,
                config,
                highLabelId,
                highRelationshipTypeId,
                countsUpdater,
                cacheFactory,
                progressListener,
                contextFactory,
                storeCursorsCreator,
                memoryTracker));
    }
}
