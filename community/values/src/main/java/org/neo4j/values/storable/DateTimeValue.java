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
package org.neo4j.values.storable;

import static java.time.Instant.ofEpochMilli;
import static java.time.Instant.ofEpochSecond;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.util.Objects.requireNonNull;
import static org.neo4j.memory.HeapEstimator.ZONED_DATE_TIME_SIZE;
import static org.neo4j.memory.HeapEstimator.shallowSizeOfInstance;
import static org.neo4j.values.storable.DateValue.DATE_PATTERN;
import static org.neo4j.values.storable.DateValue.parseDate;
import static org.neo4j.values.storable.IntegralValue.safeCastIntegral;
import static org.neo4j.values.storable.LocalDateTimeValue.optTime;
import static org.neo4j.values.storable.TimeValue.OFFSET;
import static org.neo4j.values.storable.TimeValue.TIME_PATTERN;
import static org.neo4j.values.storable.TimeValue.parseOffset;
import static org.neo4j.values.storable.Values.NO_VALUE;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalUnit;
import java.time.zone.ZoneRulesException;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.neo4j.exceptions.InvalidArgumentException;
import org.neo4j.exceptions.TemporalParseException;
import org.neo4j.exceptions.UnsupportedTemporalUnitException;
import org.neo4j.internal.helpers.collection.Pair;
import org.neo4j.values.AnyValue;
import org.neo4j.values.StructureBuilder;
import org.neo4j.values.ValueMapper;
import org.neo4j.values.virtual.MapValue;

public final class DateTimeValue extends TemporalValue<ZonedDateTime, DateTimeValue> {
    private static final long INSTANCE_SIZE = shallowSizeOfInstance(DateTimeValue.class) + ZONED_DATE_TIME_SIZE;

    public static final DateTimeValue MIN_VALUE =
            new DateTimeValue(ZonedDateTime.of(LocalDateTime.MIN, ZoneOffset.MIN));
    public static final DateTimeValue MAX_VALUE =
            new DateTimeValue(ZonedDateTime.of(LocalDateTime.MAX, ZoneOffset.MAX));

    private final ZonedDateTime value;
    private final long epochSeconds;

    private DateTimeValue(ZonedDateTime value) {
        ZoneId zone = value.getZone().normalized();
        if (zone instanceof ZoneOffset) {
            this.value = value;
        } else {
            // Do a 2-way lookup of the zone to make sure we only use the new name of renamed zones
            ZoneId mappedZone = ZoneId.of(TimeZones.map(TimeZones.map(zone.getId())));
            this.value = value.withZoneSameInstant(mappedZone);
        }
        this.epochSeconds = this.value.toEpochSecond();
    }

    public static DateTimeValue datetime(DateValue date, LocalTimeValue time, ZoneId zone) {
        return new DateTimeValue(ZonedDateTime.of(date.temporal(), time.temporal(), zone));
    }

    public static DateTimeValue datetime(DateValue date, TimeValue time) {
        OffsetTime t = time.temporal();
        return new DateTimeValue(ZonedDateTime.of(date.temporal(), t.toLocalTime(), t.getOffset()));
    }

    public static DateTimeValue datetime(
            int year, int month, int day, int hour, int minute, int second, int nanoOfSecond, String zone) {
        return datetime(year, month, day, hour, minute, second, nanoOfSecond, parseZoneName(zone));
    }

    public static DateTimeValue datetime(
            int year, int month, int day, int hour, int minute, int second, int nanoOfSecond, ZoneId zone) {
        return new DateTimeValue(assertValidArgument(
                () -> ZonedDateTime.of(year, month, day, hour, minute, second, nanoOfSecond, zone)));
    }

    public static DateTimeValue datetime(long epochSecond, long nano, ZoneOffset zoneOffset) {
        return new DateTimeValue(datetimeRaw(epochSecond, nano, zoneOffset));
    }

    public static ZonedDateTime datetimeRaw(long epochSecond, long nano, ZoneOffset zoneOffset) {
        return datetimeRaw(epochSecond, nano, (ZoneId) zoneOffset);
    }

    public static DateTimeValue datetime(ZonedDateTime datetime) {
        return new DateTimeValue(requireNonNull(datetime, "ZonedDateTime"));
    }

    public static DateTimeValue datetime(OffsetDateTime datetime) {
        return new DateTimeValue(requireNonNull(datetime, "OffsetDateTime").toZonedDateTime());
    }

    public static DateTimeValue datetime(long epochSecondUTC, long nano, ZoneId zone) {
        return new DateTimeValue(datetimeRaw(epochSecondUTC, nano, zone));
    }

    public static ZonedDateTime datetimeRaw(long epochSecondUTC, long nano, ZoneId zone) {
        return assertValidArgument(() -> ofInstant(ofEpochSecond(epochSecondUTC, nano), zone));
    }

    public static DateTimeValue ofEpoch(IntegralValue epochSecondUTC, IntegralValue nano) {
        long ns = safeCastIntegral("nanosecond", nano, 0);
        final long upperNano = 1000_000_000;
        if (ns < 0 || ns >= upperNano) {
            throw InvalidArgumentException.invalidNanosecond(upperNano - 1, ns);
        }
        return new DateTimeValue(datetimeRaw(epochSecondUTC.longValue(), ns, UTC));
    }

    public static DateTimeValue ofEpochMillis(IntegralValue millisUTC) {
        return new DateTimeValue(assertValidArgument(() -> ofInstant(ofEpochMilli(millisUTC.longValue()), UTC)));
    }

    public static DateTimeValue parse(
            CharSequence text, Supplier<ZoneId> defaultZone, CSVHeaderInformation fieldsFromHeader) {
        if (fieldsFromHeader != null) {
            if (!(fieldsFromHeader instanceof TimeCSVHeaderInformation)) {
                throw new IllegalStateException("Wrong header information type: " + fieldsFromHeader);
            }
            // Override defaultZone
            defaultZone = ((TimeCSVHeaderInformation) fieldsFromHeader).zoneSupplier(defaultZone);
        }
        return parse(DateTimeValue.class, PATTERN, DateTimeValue::parse, text, defaultZone);
    }

    public static DateTimeValue parse(CharSequence text, Supplier<ZoneId> defaultZone) {
        return parse(DateTimeValue.class, PATTERN, DateTimeValue::parse, text, defaultZone);
    }

    public static DateTimeValue parse(TextValue text, Supplier<ZoneId> defaultZone) {
        return parse(DateTimeValue.class, PATTERN, DateTimeValue::parse, text, defaultZone);
    }

    public static DateTimeValue now(Clock clock) {
        return new DateTimeValue(ZonedDateTime.now(clock));
    }

    public static DateTimeValue now(Clock clock, String timezone) {
        return now(clock.withZone(parseZoneName(timezone)));
    }

    public static DateTimeValue now(Clock clock, Supplier<ZoneId> defaultZone) {
        return now(clock.withZone(defaultZone.get()));
    }

    public static DateTimeValue build(MapValue map, Supplier<ZoneId> defaultZone) {
        return StructureBuilder.build(builder(defaultZone), map);
    }

    public static DateTimeValue select(AnyValue from, Supplier<ZoneId> defaultZone) {
        return builder(defaultZone).selectDateTime(from);
    }

    public static DateTimeValue truncate(
            TemporalUnit unit, TemporalValue input, MapValue fields, Supplier<ZoneId> defaultZone) {
        Pair<LocalDate, LocalTime> pair = getTruncatedDateAndTime(unit, input, "date time");

        LocalDate truncatedDate = pair.first();
        LocalTime truncatedTime = pair.other();

        ZoneId zoneId = input.supportsTimeZone() ? input.getZoneId(defaultZone) : defaultZone.get();
        ZonedDateTime truncatedZDT = ZonedDateTime.of(truncatedDate, truncatedTime, zoneId);

        if (fields.size() == 0) {
            return datetime(truncatedZDT);
        } else {
            // Timezone needs some special handling, since the builder will shift keeping the instant instead of the
            // local time
            AnyValue timezone = fields.get("timezone");
            if (timezone != NO_VALUE) {
                truncatedZDT = truncatedZDT.withZoneSameLocal(timezoneOf(timezone));
            }

            return updateFieldMapWithConflictingSubseconds(fields, unit, truncatedZDT, (mapValue, zonedDateTime) -> {
                if (mapValue.size() == 0) {
                    return datetime(zonedDateTime);
                } else {
                    return build(mapValue.updatedWith("datetime", datetime(zonedDateTime)), defaultZone);
                }
            });
        }
    }

    static DateTimeBuilder<DateTimeValue> builder(Supplier<ZoneId> defaultZone) {
        return new DateTimeBuilder<>(defaultZone) {
            @Override
            protected boolean supportsTimeZone() {
                return true;
            }

            @Override
            protected boolean supportsEpoch() {
                return true;
            }

            private final ZonedDateTime defaultZonedDateTime = ZonedDateTime.of(
                    TemporalFields.year.defaultValue, TemporalFields.month.defaultValue,
                    TemporalFields.day.defaultValue, TemporalFields.hour.defaultValue,
                    TemporalFields.minute.defaultValue, TemporalFields.second.defaultValue,
                    TemporalFields.nanosecond.defaultValue, timezone());

            @Override
            public DateTimeValue buildInternal() {
                boolean selectingDate = fields.containsKey(TemporalFields.date);
                boolean selectingTime = fields.containsKey(TemporalFields.time);
                boolean selectingDateTime = fields.containsKey(TemporalFields.datetime);
                boolean selectingEpoch = fields.containsKey(TemporalFields.epochSeconds)
                        || fields.containsKey(TemporalFields.epochMillis);
                boolean selectingTimeZone;
                ZonedDateTime result;
                if (selectingDateTime) {
                    AnyValue dtField = fields.get(TemporalFields.datetime);
                    if (!(dtField instanceof TemporalValue dt)) {
                        String prettyVal = dtField instanceof Value v ? v.prettyPrint() : String.valueOf(dtField);
                        throw InvalidArgumentException.cannotConstructTemporal(
                                "date time", String.valueOf(dtField), prettyVal);
                    }
                    LocalTime timePart = dt.getTimePart(defaultZone).toLocalTime();
                    ZoneId zoneId = dt.getZoneId(defaultZone);
                    result = ZonedDateTime.of(dt.getDatePart(), timePart, zoneId);
                    selectingTimeZone = dt.supportsTimeZone();
                } else if (selectingEpoch) {
                    if (fields.containsKey(TemporalFields.epochSeconds)) {
                        AnyValue epochField = fields.get(TemporalFields.epochSeconds);
                        if (!(epochField instanceof IntegralValue epochSeconds)) {
                            String prettyVal =
                                    epochField instanceof Value v ? v.prettyPrint() : String.valueOf(epochField);
                            throw InvalidArgumentException.cannotConstructTemporal(
                                    "date time", String.valueOf(epochField), prettyVal);
                        }
                        result = assertValidArgument(() -> ZonedDateTime.ofInstant(
                                Instant.ofEpochMilli(epochSeconds.longValue() * 1000), timezone()));
                    } else {
                        AnyValue epochField = fields.get(TemporalFields.epochMillis);
                        if (!(epochField instanceof IntegralValue epochMillis)) {
                            String prettyVal =
                                    epochField instanceof Value v ? v.prettyPrint() : String.valueOf(epochField);
                            throw InvalidArgumentException.cannotConstructTemporal(
                                    "date time", String.valueOf(epochField), prettyVal);
                        }
                        result = assertValidArgument(() ->
                                ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis.longValue()), timezone()));
                    }
                    selectingTimeZone = false;
                } else if (selectingTime || selectingDate) {

                    LocalTime time;
                    ZoneId zoneId;
                    if (selectingTime) {
                        AnyValue timeField = fields.get(TemporalFields.time);
                        if (!(timeField instanceof TemporalValue t)) {
                            String prettyVal =
                                    timeField instanceof Value v ? v.prettyPrint() : String.valueOf(timeField);
                            throw InvalidArgumentException.cannotConstructTemporal(
                                    "time", String.valueOf(timeField), prettyVal);
                        }
                        time = t.getTimePart(defaultZone).toLocalTime();
                        zoneId = t.getZoneId(defaultZone);
                        selectingTimeZone = t.supportsTimeZone();
                    } else {
                        time = LocalTimeValue.DEFAULT_LOCAL_TIME;
                        zoneId = timezone();
                        selectingTimeZone = false;
                    }
                    LocalDate date;
                    if (selectingDate) {
                        AnyValue dateField = fields.get(TemporalFields.date);
                        if (!(dateField instanceof TemporalValue t)) {
                            String prettyVal =
                                    dateField instanceof Value v ? v.prettyPrint() : String.valueOf(dateField);
                            throw InvalidArgumentException.cannotConstructTemporal(
                                    "date", String.valueOf(dateField), prettyVal);
                        }
                        date = t.getDatePart();
                    } else {
                        date = DateValue.DEFAULT_CALENDER_DATE;
                    }
                    result = ZonedDateTime.of(date, time, zoneId);
                } else {
                    result = defaultZonedDateTime;
                    selectingTimeZone = false;
                }

                if (fields.containsKey(TemporalFields.week)
                        && !selectingDate
                        && !selectingDateTime
                        && !selectingEpoch) {
                    // Be sure to be in the start of the week based year (which can be later than 1st Jan)
                    result = result.with(
                                    IsoFields.WEEK_BASED_YEAR,
                                    safeCastIntegral(
                                            TemporalFields.year.name(),
                                            fields.get(TemporalFields.year),
                                            TemporalFields.year.defaultValue))
                            .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, 1)
                            .with(ChronoField.DAY_OF_WEEK, 1);
                }

                result = assignAllFields(result);
                if (timezone != null) {
                    if (((selectingTime || selectingDateTime) && selectingTimeZone) || selectingEpoch) {
                        try {
                            result = result.withZoneSameInstant(timezone());
                        } catch (DateTimeParseException e) {
                            String prettyVal = timezone instanceof Value v ? v.prettyPrint() : String.valueOf(timezone);
                            throw TemporalParseException.failedToProcessDateTime(prettyVal, e);
                        }
                    } else {
                        result = result.withZoneSameLocal(timezone());
                    }
                }
                return datetime(result);
            }

            @Override
            protected DateTimeValue selectDateTime(AnyValue datetime) {
                if (datetime instanceof DateTimeValue value) {
                    ZoneId zone = optionalTimezone();
                    return zone == null
                            ? value
                            : new DateTimeValue(
                                    ZonedDateTime.of(value.temporal().toLocalDateTime(), zone));
                }
                if (datetime instanceof LocalDateTimeValue value) {
                    return new DateTimeValue(ZonedDateTime.of(value.temporal(), timezone()));
                }
                throw new UnsupportedTemporalUnitException("Cannot select datetime from: " + datetime);
            }
        };
    }

    @Override
    ZonedDateTime temporal() {
        return value;
    }

    @Override
    LocalDate getDatePart() {
        return value.toLocalDate();
    }

    @Override
    LocalTime getLocalTimePart() {
        return value.toLocalTime();
    }

    @Override
    OffsetTime getTimePart(Supplier<ZoneId> defaultZone) {
        ZoneOffset offset = value.getOffset();
        LocalTime localTime = value.toLocalTime();
        return OffsetTime.of(localTime, offset);
    }

    @Override
    ZoneId getZoneId(Supplier<ZoneId> defaultZone) {
        return value.getZone();
    }

    @Override
    ZoneOffset getZoneOffset() {
        return value.getOffset();
    }

    @Override
    public boolean supportsTimeZone() {
        return true;
    }

    @Override
    boolean hasTime() {
        return true;
    }

    @Override
    public boolean equals(Value other) {
        if (other instanceof DateTimeValue dateTimeValue) {
            ZonedDateTime that = dateTimeValue.value;
            boolean res = value.toLocalDateTime().equals(that.toLocalDateTime());
            if (res) {
                ZoneId thisZone = value.getZone();
                ZoneId thatZone = that.getZone();
                boolean thisIsOffset = thisZone instanceof ZoneOffset;
                boolean thatIsOffset = thatZone instanceof ZoneOffset;
                if (thisIsOffset && thatIsOffset) {
                    res = thisZone.equals(thatZone);
                } else if (!thisIsOffset && !thatIsOffset) {
                    res = TimeZones.map(thisZone.getId()) == TimeZones.map(thatZone.getId());
                } else {
                    res = false;
                }
            }
            return res;
        }
        return false;
    }

    @Override
    public <E extends Exception> void writeTo(ValueWriter<E> writer) throws E {
        writer.writeDateTime(value);
    }

    @Override
    protected int unsafeCompareTo(Value other) {
        DateTimeValue that = (DateTimeValue) other;
        int cmp = Long.compare(epochSeconds, that.epochSeconds);
        if (cmp == 0) {
            cmp = value.toLocalTime().getNano() - that.value.toLocalTime().getNano();
            if (cmp == 0) {
                ZoneOffset thisOffset = value.getOffset();
                ZoneOffset thatOffset = that.value.getOffset();

                cmp = Integer.compare(thisOffset.getTotalSeconds(), thatOffset.getTotalSeconds());
                if (cmp == 0) {
                    ZoneId thisZone = value.getZone();
                    ZoneId thatZone = that.value.getZone();
                    boolean thisIsOffset = thisZone instanceof ZoneOffset;
                    boolean thatIsOffset = thatZone instanceof ZoneOffset;
                    // non-named timezone (just offset) before named-time zones, alphabetically
                    cmp = Boolean.compare(thatIsOffset, thisIsOffset);
                    if (cmp == 0) {
                        if (!thisIsOffset) // => also means !thatIsOffset
                        {
                            cmp = compareNamedZonesWithMapping(thisZone, thatZone);
                        }
                    }
                    if (cmp == 0) {
                        cmp = value.getChronology().compareTo(that.value.getChronology());
                    }
                }
            }
        }
        return cmp;
    }

    private static int compareNamedZonesWithMapping(ZoneId thisZone, ZoneId thatZone) {
        String thisZoneNormalized = TimeZones.map(TimeZones.map(thisZone.getId()));
        String thatZoneNormalized = TimeZones.map(TimeZones.map(thatZone.getId()));
        return thisZoneNormalized.compareTo(thatZoneNormalized);
    }

    @Override
    public String prettyPrint() {
        return assertPrintable(String.valueOf(value), () -> value.format(DateTimeFormatter.ISO_DATE_TIME));
    }

    @Override
    public ValueRepresentation valueRepresentation() {
        return ValueRepresentation.ZONED_DATE_TIME;
    }

    @Override
    protected int computeHashToMemoize() {
        return value.toInstant().hashCode();
    }

    @Override
    public String getTypeName() {
        return "DateTime";
    }

    @Override
    public <T> T map(ValueMapper<T> mapper) {
        return mapper.mapDateTime(this);
    }

    @Override
    public DateTimeValue add(DurationValue duration) {
        return replacement(assertValidArithmetic(() -> value.plus(duration)));
    }

    @Override
    public DateTimeValue sub(DurationValue duration) {
        return replacement(assertValidArithmetic(() -> value.minus(duration)));
    }

    @Override
    DateTimeValue replacement(ZonedDateTime datetime) {
        return value == datetime ? this : new DateTimeValue(datetime);
    }

    @Override
    public long estimatedHeapUsage() {
        return INSTANCE_SIZE;
    }

    private static final Pattern PATTERN =
            Pattern.compile(DATE_PATTERN + "(?<time>T" + TIME_PATTERN + ")?", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter ZONE_NAME_PARSER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendZoneRegionId()
            .toFormatter();

    private static DateTimeValue parse(Matcher matcher, Supplier<ZoneId> defaultZone) {
        LocalDateTime local = LocalDateTime.of(parseDate(matcher), optTime(matcher));
        String zoneName = matcher.group("zoneName");
        ZoneOffset offset = parseOffset(matcher);
        ZoneId zone;
        if (zoneName != null) {
            zone = parseZoneName(zoneName);
            if (offset != null) {
                try {
                    if (!zone.getRules().isValidOffset(local, offset)) {
                        String actualOffset = String.valueOf(offset);
                        List<String> validOffsets = zone.getRules().getValidOffsets(local).stream()
                                .map(String::valueOf)
                                .toList();
                        throw InvalidArgumentException.timezoneAndOffsetMismatch(
                                zoneName, actualOffset, validOffsets, matcher.group());
                    }
                } catch (ZoneRulesException e) {
                    throw TemporalParseException.cannotProcessCause(matcher.group(), e);
                }
            }
        } else if (offset != null) {
            zone = offset;
        } else {
            zone = defaultZone.get();
        }
        return new DateTimeValue(ZonedDateTime.ofLocal(local, zone, offset));
    }

    static ZoneId parseZoneName(String zoneName) {
        ZoneId parsedName;
        try {
            parsedName = ZONE_NAME_PARSER.parse(zoneName.replace(' ', '_')).query(TemporalQueries.zoneId());
        } catch (DateTimeParseException e) {
            throw TemporalParseException.invalidTimeZone(zoneName, e);
        }
        return parsedName;
    }

    public static ZoneId parseZoneOffsetOrZoneName(String zoneName) {
        Matcher matcher = OFFSET.matcher(zoneName);
        if (matcher.matches()) {
            return parseOffset(matcher);
        }
        try {
            return ZONE_NAME_PARSER.parse(zoneName.replace(' ', '_')).query(TemporalQueries.zoneId());
        } catch (DateTimeParseException e) {
            throw TemporalParseException.invalidTimeZone(zoneName, e);
        }
    }

    abstract static class DateTimeBuilder<Result> extends Builder<Result> {
        DateTimeBuilder(Supplier<ZoneId> defaultZone) {
            super(defaultZone);
        }

        @Override
        protected final boolean supportsDate() {
            return true;
        }

        @Override
        protected final boolean supportsTime() {
            return true;
        }

        protected abstract Result selectDateTime(AnyValue date);
    }
}
