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
package org.neo4j.io.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * Thrown when reading from a {@link ReadableByteChannel} into a buffer
 * and not enough bytes ({@link ByteBuffer#limit()}) could be read.
 * Also used when reading from {@link ReadableChannel}.
 */
public final class ReadPastEndException extends IOException {
    public static final ReadPastEndException INSTANCE = new ReadPastEndException();

    private ReadPastEndException() {}

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this; // only used as a static instance, stack trace will confuse
    }
}
