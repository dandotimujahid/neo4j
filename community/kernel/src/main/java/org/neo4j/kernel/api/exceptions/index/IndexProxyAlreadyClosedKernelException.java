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
package org.neo4j.kernel.api.exceptions.index;

import org.neo4j.exceptions.KernelException;
import org.neo4j.gqlstatus.ErrorGqlStatusObject;
import org.neo4j.kernel.api.exceptions.Status;

public final class IndexProxyAlreadyClosedKernelException extends KernelException {
    public IndexProxyAlreadyClosedKernelException(Class<?> proxyClazz) {
        super(
                Status.Schema.IndexCreationFailed,
                "%s has been closed. No more interactions allowed.",
                proxyClazz.getSimpleName());
    }

    public IndexProxyAlreadyClosedKernelException(ErrorGqlStatusObject gqlStatusObject, Class<?> proxyClazz) {
        super(
                gqlStatusObject,
                Status.Schema.IndexCreationFailed,
                "%s has been closed. No more interactions allowed.",
                proxyClazz.getSimpleName());
    }
}
