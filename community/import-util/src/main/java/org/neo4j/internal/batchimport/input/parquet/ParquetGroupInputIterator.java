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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.neo4j.batchimport.api.InputIterator;
import org.neo4j.batchimport.api.input.IdType;
import org.neo4j.batchimport.api.input.InputChunk;
import org.neo4j.internal.batchimport.input.Groups;

class ParquetGroupInputIterator implements InputIterator {

    private final ParquetDataFactory source;
    private final Groups groups;
    private final IdType idType;
    private final ParquetMonitor monitor;
    private final Map<Path, List<ParquetColumn>> columnInfo;
    private final String arrayDelimiter;
    private ParquetInputIterator current;

    ParquetGroupInputIterator(
            ParquetDataFactory source,
            Groups groups,
            IdType idType,
            String arrayDelimiter,
            Map<Path, List<ParquetColumn>> columnInfo,
            ParquetMonitor monitor) {
        this.monitor = monitor;
        this.source = source;
        this.groups = groups;
        this.idType = idType;
        this.arrayDelimiter = arrayDelimiter;
        this.columnInfo = columnInfo;
    }

    @Override
    public InputChunk newChunk() {
        return new ParquetProxyInputChunk();
    }

    @Override
    public synchronized boolean next(InputChunk chunk) throws IOException {
        while (true) {
            if (!source.hasNextFile()) {
                return false;
            }
            if (current == null) {
                ParquetData data = source.getNextFile();
                current = new ParquetInputIterator(
                        data, groups, idType, columnInfo, data.defaultTimezoneSupplier(), arrayDelimiter);
            }

            if (current.next((ParquetInputChunk) chunk)) {
                return true;
            }
            current.close();
            current = null;
        }
    }

    @Override
    public void close() {
        if (current != null) {
            try {
                current.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
