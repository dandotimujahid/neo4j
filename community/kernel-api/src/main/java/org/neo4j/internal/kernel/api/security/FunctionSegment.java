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
package org.neo4j.internal.kernel.api.security;

import org.neo4j.internal.helpers.NameUtil;

public record FunctionSegment(String function) implements Segment {

    @Override
    public boolean satisfies(Segment segment) {
        if (segment instanceof FunctionSegment other) {
            return function == null || function.equals(other.function);
        }
        return false;
    }

    @Override
    public String toCypherSnippet() {
        if (function == null) {
            return "*";
        } else {
            return NameUtil.escapeGlob(function);
        }
    }

    @Override
    public String toString() {
        return String.format("FUNCTION(%s)", nullToStar(function));
    }

    public static final FunctionSegment ALL = new FunctionSegment(null);
}
