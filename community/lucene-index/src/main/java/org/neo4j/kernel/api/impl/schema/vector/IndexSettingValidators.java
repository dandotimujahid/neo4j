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
package org.neo4j.kernel.api.impl.schema.vector;

import static org.neo4j.kernel.api.impl.schema.vector.VectorIndexConfigUtils.missing;

import java.util.Locale;
import java.util.OptionalInt;
import org.eclipse.collections.api.map.MapIterable;
import org.neo4j.graphdb.schema.IndexSetting;
import org.neo4j.internal.schema.IndexConfigValidationRecords.IncorrectType;
import org.neo4j.internal.schema.IndexConfigValidationRecords.IndexConfigValidationRecord;
import org.neo4j.internal.schema.IndexConfigValidationRecords.InvalidValue;
import org.neo4j.internal.schema.IndexConfigValidationRecords.MissingSetting;
import org.neo4j.internal.schema.IndexConfigValidationRecords.Pending;
import org.neo4j.internal.schema.IndexConfigValidationRecords.UnrecognizedSetting;
import org.neo4j.internal.schema.IndexConfigValidationRecords.Valid;
import org.neo4j.internal.schema.SettingsAccessor;
import org.neo4j.kernel.api.impl.schema.vector.VectorIndexConfigUtils.Range;
import org.neo4j.kernel.api.vector.VectorQuantization;
import org.neo4j.kernel.api.vector.VectorSimilarityFunction;
import org.neo4j.values.storable.IntegralValue;
import org.neo4j.values.storable.NoValue;
import org.neo4j.values.storable.TextValue;
import org.neo4j.values.storable.Value;
import org.neo4j.values.storable.Values;

class IndexSettingValidators {
    abstract static class IndexSettingValidator<VALUE extends Value, TYPE> {
        protected final IndexSetting setting;
        protected final TYPE createDefault;
        protected final TYPE readDefault;

        protected IndexSettingValidator(IndexSetting setting) {
            this(setting, null);
        }

        protected IndexSettingValidator(IndexSetting setting, TYPE defaultValue) {
            this(setting, defaultValue, defaultValue);
        }

        protected IndexSettingValidator(IndexSetting setting, TYPE readDefault, TYPE createDefault) {
            this.setting = setting;
            this.readDefault = readDefault;
            this.createDefault = createDefault;
        }

        abstract TYPE map(VALUE value);

        abstract Value map(TYPE value);

        abstract IndexConfigValidationRecord validate(SettingsAccessor accessor);

        protected IndexConfigValidationRecord extractOrDefault(SettingsAccessor accessor) {
            final var rawValue = accessor.get(setting);
            if (!missing(rawValue)) {
                return new Pending(setting, rawValue);
            }
            if (createDefault == null) {
                return new MissingSetting(setting);
            }
            return new Valid(setting, createDefault, map(createDefault));
        }

        protected Valid trustIsValid(SettingsAccessor accessor) {
            final VALUE rawValue = accessor.get(setting);
            final var value = missing(rawValue) ? readDefault : map(rawValue);
            return new Valid(setting, value, map(value));
        }

        protected IndexSetting setting() {
            return setting;
        }
    }

    static class ReadDefaultOnly<TYPE> extends IndexSettingValidator<NoValue, TYPE> {
        protected ReadDefaultOnly(IndexSetting setting, TYPE readDefault) {
            super(setting, readDefault, null);
        }

        @Override
        TYPE map(NoValue value) {
            return readDefault;
        }

        @Override
        NoValue map(TYPE value) {
            return NoValue.NO_VALUE;
        }

        @Override
        IndexConfigValidationRecord validate(SettingsAccessor accessor) {
            final var rawValue = accessor.get(setting);
            if (!missing(rawValue)) {
                return new UnrecognizedSetting(setting.getSettingName());
            }
            return new Valid(setting, readDefault, map(readDefault));
        }
    }

    static final class IntegerValidator extends IndexSettingValidator<IntegralValue, Integer> {
        private final Range<Integer> supportedRange;

        IntegerValidator(IndexSetting setting, Range<Integer> supportedRange) {
            this(setting, null, supportedRange);
        }

        IntegerValidator(IndexSetting setting, Integer defaultValue, Range<Integer> supportedRange) {
            super(setting, defaultValue);
            this.supportedRange = supportedRange;
            assert defaultValue == null || supportedRange.contains(defaultValue);
        }

        @Override
        Integer map(IntegralValue value) {
            return (int) value.longValue();
        }

        @Override
        Value map(Integer value) {
            return Values.intValue(value);
        }

        @Override
        IndexConfigValidationRecord validate(SettingsAccessor accessor) {
            final var record = extractOrDefault(accessor);
            if (!(record instanceof final Pending pending)) {
                return record;
            }

            if (!(pending.rawValue() instanceof final IntegralValue integralValue)) {
                return new IncorrectType(pending, IntegralValue.class);
            }

            final var value = map(integralValue);
            return supportedRange.contains(value)
                    ? new Valid(setting, value, map(value))
                    : new InvalidValue(pending, value, supportedRange);
        }
    }

    static final class OptionalIntSettingValidator extends IndexSettingValidator<IntegralValue, OptionalInt> {
        private final Range<Integer> supportedRange;

        OptionalIntSettingValidator(IndexSetting setting, Range<Integer> supportedRange) {
            this(setting, supportedRange, null);
        }

        OptionalIntSettingValidator(IndexSetting setting, Range<Integer> supportedRange, OptionalInt defaultValue) {
            super(setting, defaultValue);
            this.supportedRange = supportedRange;
            assert defaultValue == null || defaultValue.isEmpty() || supportedRange.contains(defaultValue.getAsInt());
        }

        @Override
        OptionalInt map(IntegralValue dimensions) {
            return OptionalInt.of((int) dimensions.longValue());
        }

        @Override
        Value map(OptionalInt dimensions) {
            return dimensions.isPresent() ? Values.intValue(dimensions.getAsInt()) : NoValue.NO_VALUE;
        }

        @Override
        IndexConfigValidationRecord validate(SettingsAccessor accessor) {
            final var record = extractOrDefault(accessor);
            if (!(record instanceof final Pending pending)) {
                return record;
            }

            if (!(pending.rawValue() instanceof final IntegralValue integralValue)) {
                return new IncorrectType(pending, IntegralValue.class);
            }

            final var dimensions = map(integralValue);
            return supportedRange.contains(dimensions.orElseThrow(() ->
                            new IllegalStateException("'%s' should not be empty at this point.".formatted(setting))))
                    ? new Valid(setting, dimensions, map(dimensions))
                    : new InvalidValue(pending, dimensions, supportedRange);
        }
    }

    static final class SimilarityFunctionValidator extends IndexSettingValidator<TextValue, VectorSimilarityFunction> {
        private final MapIterable<String, VectorSimilarityFunction> similarityFunctions;

        SimilarityFunctionValidator(MapIterable<String, VectorSimilarityFunction> supportedSimilarityFunctions) {
            this(supportedSimilarityFunctions, null);
        }

        SimilarityFunctionValidator(
                MapIterable<String, VectorSimilarityFunction> supportedSimilarityFunctions,
                VectorSimilarityFunction defaultSimilarityFunction) {
            super(IndexSetting.vector_Similarity_Function(), defaultSimilarityFunction);
            this.similarityFunctions = supportedSimilarityFunctions;
            assert defaultSimilarityFunction == null
                    || supportedSimilarityFunctions.containsValue(defaultSimilarityFunction);
        }

        @Override
        VectorSimilarityFunction map(TextValue textValue) {
            return similarityFunctions.get(textValue.stringValue().toUpperCase(Locale.ROOT));
        }

        @Override
        Value map(VectorSimilarityFunction similarityFunction) {
            return Values.stringValue(similarityFunction.name());
        }

        @Override
        IndexConfigValidationRecord validate(SettingsAccessor accessor) {
            final var record = extractOrDefault(accessor);
            if (!(record instanceof final Pending pending)) {
                return record;
            }

            if (!(pending.rawValue() instanceof final TextValue textValue)) {
                return new IncorrectType(pending, TextValue.class);
            }

            final var similarityFunction = map(textValue);
            return similarityFunction == null
                    ? new InvalidValue(pending, similarityFunctions.keysView())
                    : new Valid(setting, similarityFunction, map(similarityFunction));
        }
    }

    static final class QuantizationValidator extends IndexSettingValidator<TextValue, VectorQuantization> {
        private final MapIterable<String, VectorQuantization> quantizations;

        QuantizationValidator(
                MapIterable<String, VectorQuantization> supportedQuantizations,
                VectorQuantization readDefaultQuantization,
                VectorQuantization writeDefaultQuantization) {
            super(IndexSetting.vector_Quantization(), readDefaultQuantization, writeDefaultQuantization);
            this.quantizations = supportedQuantizations;
            assert supportedQuantizations == null
                    || supportedQuantizations.containsValue(readDefaultQuantization)
                            && supportedQuantizations.containsValue(writeDefaultQuantization);
        }

        @Override
        VectorQuantization map(TextValue textValue) {
            return quantizations.get(textValue.stringValue().toUpperCase(Locale.ROOT));
        }

        @Override
        Value map(VectorQuantization quantization) {
            return Values.stringValue(quantization.name());
        }

        @Override
        IndexConfigValidationRecord validate(SettingsAccessor accessor) {
            final var record = extractOrDefault(accessor);
            if (!(record instanceof final Pending pending)) {
                return record;
            }

            if (!(pending.rawValue() instanceof final TextValue textValue)) {
                return new IncorrectType(pending, TextValue.class);
            }

            final var quantization = map(textValue);
            return quantization == null
                    ? new InvalidValue(pending, quantizations.keysView())
                    : new Valid(setting, quantization, map(quantization));
        }
    }
}
