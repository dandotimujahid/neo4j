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
package org.neo4j.kernel.monitoring.tracing;

import java.time.Clock;
import org.neo4j.configuration.Config;
import org.neo4j.io.pagecache.tracing.PageCacheTracer;
import org.neo4j.kernel.database.NamedDatabaseId;
import org.neo4j.kernel.impl.transaction.tracing.DatabaseTracer;
import org.neo4j.lock.LockTracer;
import org.neo4j.logging.InternalLog;
import org.neo4j.monitoring.Monitors;
import org.neo4j.scheduler.JobScheduler;
import org.neo4j.time.SystemNanoClock;

public class NullTracersFactory implements TracerFactory {

    static final String NULL_TRACERS_NAME = "null";

    @Override
    public String getName() {
        return NULL_TRACERS_NAME;
    }

    @Override
    public PageCacheTracer createPageCacheTracer(
            Monitors monitors, JobScheduler jobScheduler, SystemNanoClock clock, InternalLog log, Config config) {
        return PageCacheTracer.NULL;
    }

    @Override
    public DatabaseTracer createDatabaseTracer(
            PageCacheTracer pageCacheTracer, Clock clock, InternalLog log, NamedDatabaseId namedDatabaseId) {
        return DatabaseTracer.NULL;
    }

    @Override
    public LockTracer createLockTracer(Clock clock) {
        return LockTracer.NONE;
    }
}
