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
package org.neo4j.procedure.impl.temporal;

import static java.util.Collections.singletonList;
import static org.neo4j.internal.kernel.api.procs.DefaultParameterValue.nullValue;
import static org.neo4j.internal.kernel.api.procs.FieldSignature.inputField;
import static org.neo4j.internal.kernel.api.procs.Neo4jTypes.NTDateTime;

import java.time.Clock;
import java.time.ZoneId;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import org.neo4j.cypher.internal.expressions.functions.Category;
import org.neo4j.internal.kernel.api.exceptions.ProcedureException;
import org.neo4j.internal.kernel.api.procs.FieldSignature;
import org.neo4j.internal.kernel.api.procs.Neo4jTypes;
import org.neo4j.internal.kernel.api.procs.QualifiedName;
import org.neo4j.internal.kernel.api.procs.UserFunctionSignature;
import org.neo4j.kernel.api.procedure.CallableUserFunction;
import org.neo4j.kernel.api.procedure.Context;
import org.neo4j.kernel.api.procedure.GlobalProcedures;
import org.neo4j.procedure.Description;
import org.neo4j.values.AnyValue;
import org.neo4j.values.storable.DateTimeValue;
import org.neo4j.values.storable.IntegralValue;
import org.neo4j.values.storable.TemporalValue;
import org.neo4j.values.storable.TextValue;
import org.neo4j.values.virtual.MapValue;

@Description("Creates a `ZONED DATETIME` instant.")
class DateTimeFunction extends TemporalFunction<DateTimeValue> {
    private static final String CATEGORY = Category.TEMPORAL();

    private static final List<FieldSignature> INPUT_SIGNATURE = singletonList(
            inputField(
                    "input",
                    Neo4jTypes.NTAny,
                    DEFAULT_PARAMETER_VALUE,
                    false,
                    "Either a string representation of a temporal value, a map containing the single key 'timezone', or a map containing temporal values ('year', 'month', 'day', 'hour', 'minute', 'second', 'millisecond', 'microsecond', 'nanosecond', 'timezone', 'epochSeconds', 'epochMillis') as components."));

    DateTimeFunction(Supplier<ZoneId> defaultZone) {
        super(NTDateTime, INPUT_SIGNATURE, defaultZone);
    }

    @Override
    protected DateTimeValue now(Clock clock, String timezone, Supplier<ZoneId> defaultZone) {
        return timezone == null ? DateTimeValue.now(clock, defaultZone) : DateTimeValue.now(clock, timezone);
    }

    @Override
    protected DateTimeValue parse(TextValue value, Supplier<ZoneId> defaultZone) {
        return DateTimeValue.parse(value, defaultZone);
    }

    @Override
    protected DateTimeValue build(MapValue map, Supplier<ZoneId> defaultZone) {
        return DateTimeValue.build(map, defaultZone);
    }

    @Override
    protected DateTimeValue select(AnyValue from, Supplier<ZoneId> defaultZone) {
        return DateTimeValue.select(from, defaultZone);
    }

    @Override
    protected List<FieldSignature> getTemporalTruncateSignature() {
        return Arrays.asList(
                inputField(
                        "unit",
                        Neo4jTypes.NTString,
                        "A string representing one of the following: 'microsecond', 'millisecond', 'second', 'minute', 'hour', 'day', 'week', 'month', 'weekYear', 'quarter', 'year', 'decade', 'century', 'millennium'."),
                inputField(
                        "input",
                        Neo4jTypes.NTAny,
                        DEFAULT_PARAMETER_VALUE,
                        false,
                        "The date to be truncated using either `ZONED DATETIME`, `LOCAL DATETIME`, or `DATE`."),
                inputField(
                        "fields",
                        Neo4jTypes.NTMap,
                        nullValue(Neo4jTypes.NTMap),
                        false,
                        "A list of time components smaller than those specified in `unit` to preserve during truncation."));
    }

    @Override
    protected DateTimeValue truncate(
            TemporalUnit unit, TemporalValue input, MapValue fields, Supplier<ZoneId> defaultZone) {
        return DateTimeValue.truncate(unit, input, fields, defaultZone);
    }

    @Override
    void registerMore(GlobalProcedures globalProcedures) throws ProcedureException {
        globalProcedures.register(new FromEpoch());
        globalProcedures.register(new FromEpochMillis());
    }

    private static class FromEpoch implements CallableUserFunction {
        private static final String DESCRIPTION =
                "Creates a `ZONED DATETIME` given the seconds and nanoseconds since the start of the epoch.";
        private static final List<FieldSignature> SIGNATURE = Arrays.asList(
                inputField(
                        "seconds",
                        Neo4jTypes.NTNumber,
                        "The number of seconds from the UNIX epoch in the UTC time zone."),
                inputField(
                        "nanoseconds",
                        Neo4jTypes.NTNumber,
                        "The number of nanoseconds from the UNIX epoch in the UTC time zone. This can be added to seconds."));
        private final UserFunctionSignature signature;

        private FromEpoch() {
            this.signature = new UserFunctionSignature(
                    new QualifiedName("datetime", "fromepoch"),
                    SIGNATURE,
                    Neo4jTypes.NTDateTime,
                    false,
                    null,
                    DESCRIPTION,
                    CATEGORY,
                    true,
                    true,
                    false,
                    true);
        }

        @Override
        public UserFunctionSignature signature() {
            return signature;
        }

        @Override
        public AnyValue apply(Context ctx, AnyValue[] input) throws ProcedureException {
            if (input != null && input.length == 2) {
                if (input[0] instanceof IntegralValue seconds && input[1] instanceof IntegralValue nanoseconds) {
                    return DateTimeValue.ofEpoch(seconds, nanoseconds);
                }
                // Number of arguments are correct, but type of arguments is wrong
                throw ProcedureException.invalidCallSignature(
                        getClass().getSimpleName(),
                        this.signature.toString(),
                        "Invalid call signature for " + getClass().getSimpleName() + ": Provided input was "
                                + Arrays.toString(input));
            }
            // Number of arguments is not correct
            throw ProcedureException.invalidNumberOfProcedureOrFunctionArguments(
                    2,
                    input == null ? 0 : input.length,
                    getClass().getSimpleName(),
                    this.signature.toString(),
                    "Invalid call signature for " + getClass().getSimpleName() + ": Provided input was "
                            + Arrays.toString(input));
        }
    }

    private static class FromEpochMillis implements CallableUserFunction {
        private static final String DESCRIPTION =
                "Creates a `ZONED DATETIME` given the milliseconds since the start of the epoch.";
        private static final List<FieldSignature> SIGNATURE = Collections.singletonList(inputField(
                "milliseconds",
                Neo4jTypes.NTNumber,
                "The number of milliseconds from the UNIX epoch in the UTC time zone."));
        private final UserFunctionSignature signature;

        private FromEpochMillis() {
            this.signature = new UserFunctionSignature(
                    new QualifiedName("datetime", "fromepochmillis"),
                    SIGNATURE,
                    Neo4jTypes.NTDateTime,
                    false,
                    null,
                    DESCRIPTION,
                    CATEGORY,
                    true,
                    true,
                    false,
                    true);
        }

        @Override
        public UserFunctionSignature signature() {
            return signature;
        }

        @Override
        public AnyValue apply(Context ctx, AnyValue[] input) throws ProcedureException {
            if (input != null && input.length == 1) {
                if (input[0] instanceof IntegralValue milliseconds) {
                    return DateTimeValue.ofEpochMillis(milliseconds);
                } else {
                    // Number of arguments are correct, but type of arguments is wrong
                    throw ProcedureException.invalidCallSignature(
                            getClass().getSimpleName(),
                            this.signature.toString(),
                            "Invalid call signature for " + getClass().getSimpleName() + ": Provided input was "
                                    + Arrays.toString(input));
                }
            }
            // Number of arguments is not correct
            throw ProcedureException.invalidNumberOfProcedureOrFunctionArguments(
                    1,
                    input == null ? 0 : input.length,
                    getClass().getSimpleName(),
                    this.signature.toString(),
                    "Invalid call signature for " + getClass().getSimpleName() + ": Provided input was "
                            + Arrays.toString(input));
        }
    }

    @Override
    protected String getTemporalCypherTypeName() {
        return "ZONED DATETIME";
    }
}
