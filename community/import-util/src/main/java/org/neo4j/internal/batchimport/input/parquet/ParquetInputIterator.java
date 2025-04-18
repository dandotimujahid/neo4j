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
package org.neo4j.internal.batchimport.input.parquet;

import java.io.Closeable;
import java.io.IOException;
import java.time.ZoneId;
import java.util.function.Supplier;
import org.neo4j.batchimport.api.input.IdType;
import org.neo4j.internal.batchimport.input.Groups;

class ParquetInputIterator implements Closeable {

    private final ParquetDataReader reader;

    ParquetInputIterator(
            ParquetData parquetData,
            Groups groups,
            IdType idType,
            Supplier<ZoneId> defaultTimezoneSupplier,
            String arrayDelimiter) {
        this.reader = new ParquetDataReader(parquetData, groups, idType, defaultTimezoneSupplier, arrayDelimiter);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    public boolean next(ParquetInputChunk chunk) throws IOException {
        return chunk.readWith(reader);
    }
}
