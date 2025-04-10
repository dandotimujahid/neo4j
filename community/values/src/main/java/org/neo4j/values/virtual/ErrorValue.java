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
package org.neo4j.values.virtual;

import static org.neo4j.memory.HeapEstimator.shallowSizeOfInstance;
import static org.neo4j.memory.HeapEstimator.sizeOf;

import java.util.Comparator;
import org.neo4j.exceptions.InvalidArgumentException;
import org.neo4j.gqlstatus.ErrorClassification;
import org.neo4j.gqlstatus.ErrorGqlStatusObject;
import org.neo4j.gqlstatus.ErrorGqlStatusObjectImplementation;
import org.neo4j.gqlstatus.GqlParams;
import org.neo4j.gqlstatus.GqlStatusInfoCodes;
import org.neo4j.values.AnyValue;
import org.neo4j.values.AnyValueWriter;
import org.neo4j.values.Comparison;
import org.neo4j.values.TernaryComparator;
import org.neo4j.values.ValueMapper;
import org.neo4j.values.VirtualValue;

/**
 * The ErrorValue allow delaying errors in value creation until runtime, which is useful
 * if it turns out that the value is never used.
 */
public final class ErrorValue extends VirtualValue {
    private static final long INVALID_ARGUMENT_EXCEPTION_SHALLOW_SIZE =
            shallowSizeOfInstance(InvalidArgumentException.class);
    private static final long SHALLOW_SIZE =
            shallowSizeOfInstance(ErrorValue.class) + INVALID_ARGUMENT_EXCEPTION_SHALLOW_SIZE;
    private final InvalidArgumentException e;

    @Deprecated
    ErrorValue(Exception e) {
        this.e = new InvalidArgumentException(e.getMessage());
    }

    ErrorValue(ErrorGqlStatusObject gqlStatusObject, Exception e) {
        this.e = new InvalidArgumentException(gqlStatusObject, e.getMessage());
    }

    public static ErrorValue cannotProcess(String unprocessable, Exception e) {
        var gql = ErrorGqlStatusObjectImplementation.from(GqlStatusInfoCodes.STATUS_22000)
                .withClassification(ErrorClassification.CLIENT_ERROR)
                .withCause(ErrorGqlStatusObjectImplementation.from(GqlStatusInfoCodes.STATUS_22N11)
                        .withClassification(ErrorClassification.CLIENT_ERROR)
                        .withParam(GqlParams.StringParam.input, unprocessable)
                        .build())
                .build();
        return new ErrorValue(gql, e);
    }

    @Override
    public boolean equals(VirtualValue other) {
        throw e;
    }

    @Override
    public VirtualValueGroup valueGroup() {
        return VirtualValueGroup.ERROR;
    }

    @Override
    public int unsafeCompareTo(VirtualValue other, Comparator<AnyValue> comparator) {
        throw e;
    }

    @Override
    public Comparison unsafeTernaryCompareTo(VirtualValue other, TernaryComparator<AnyValue> comparator) {
        throw e;
    }

    @Override
    protected int computeHashToMemoize() {
        throw e;
    }

    @Override
    public <E extends Exception> void writeTo(AnyValueWriter<E> writer) {
        throw e;
    }

    @Override
    public <T> T map(ValueMapper<T> mapper) {
        throw e;
    }

    @Override
    public String getTypeName() {
        return "Error";
    }

    @Override
    public long estimatedHeapUsage() {
        return SHALLOW_SIZE
                + sizeOf(e.getMessage()); // We ignore stacktrace for now, ideally we should get rid of this whole class
    }
}
