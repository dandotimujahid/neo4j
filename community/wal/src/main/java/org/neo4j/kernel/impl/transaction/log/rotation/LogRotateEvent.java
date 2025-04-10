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
package org.neo4j.kernel.impl.transaction.log.rotation;

/**
 * Represents the event of a log rotation.
 */
public interface LogRotateEvent extends AutoCloseable {
    LogRotateEvent NULL = new LogRotateEvent() {
        @Override
        public void rotationCompleted(long rotationMillis) {}

        @Override
        public void close() {}
    };

    /**
     * Notify about completion of rotation that took {@code rotationMillis} to complete
     * @param rotationMillis transaction log rotation duration
     */
    void rotationCompleted(long rotationMillis);

    /**
     * Marks the end of the log rotation process.
     */
    @Override
    void close();
}
