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
package org.neo4j.internal.recordstorage;

import java.util.Collection;
import org.neo4j.internal.kernel.api.exceptions.TransactionFailureException;
import org.neo4j.memory.MemoryTracker;
import org.neo4j.storageengine.api.StorageCommand;

/**
 * Keeper of state that is about to be committed. That state can be {@link #extractCommands(Collection, MemoryTracker) extracted}
 * into a list of {@link Command commands}.
 */
@FunctionalInterface
public interface RecordState {

    RecordState EMPTY_RECORD_STATE = (target, memoryTracker) -> {};

    /**
     * Extracts this record state in the form of {@link Command commands} into the supplied {@code target} list.
     * @param target list that commands will be added into.
     * @throws TransactionFailureException if the state is invalid or not applicable.
     */
    void extractCommands(Collection<StorageCommand> target, MemoryTracker memoryTracker)
            throws TransactionFailureException;
}
