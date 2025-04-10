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
package org.neo4j.procedure.impl;

import java.lang.reflect.Field;
import org.neo4j.function.ThrowingFunction;
import org.neo4j.internal.kernel.api.exceptions.ProcedureException;
import org.neo4j.kernel.api.procedure.Context;

/**
 * On calling apply, injects the `value` for the field `field` on the provided `object`.
 */
public class FieldSetter {
    private final Field field;
    private final ThrowingFunction<Context, ?, ProcedureException> provider;

    FieldSetter(Field field, ThrowingFunction<Context, ?, ProcedureException> provider) {
        this.field = field;
        this.provider = provider;
    }

    public Object get(Context ctx) throws ProcedureException {
        try {
            return provider.apply(ctx);
        } catch (Throwable e) {
            throw ProcedureException.cannotInjectField(field.getName(), e);
        }
    }

    Field field() {
        return field;
    }
}
