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

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.neo4j.values.storable.DateValue.date;
import static org.neo4j.values.storable.DateValue.ordinalDate;
import static org.neo4j.values.storable.DateValue.parse;
import static org.neo4j.values.storable.DateValue.quarterDate;
import static org.neo4j.values.storable.DateValue.weekDate;
import static org.neo4j.values.utils.AnyValueTestUtil.assertEqual;
import static org.neo4j.values.utils.AnyValueTestUtil.assertNotEqual;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.neo4j.exceptions.InvalidArgumentException;
import org.neo4j.exceptions.TemporalParseException;
import org.neo4j.gqlstatus.ErrorGqlStatusObject;
import org.neo4j.gqlstatus.GqlParams;

class DateValueTest {
    @Test
    void shouldParseYear() {
        assertEquals(date(2015, 1, 1), parse("2015"));
        assertEquals(date(2015, 1, 1), parse("+2015"));
        assertEquals(date(2015, 1, 1), parse("+0002015"));
        assertCannotParse("10000");
        assertCannotParse("2K18");
    }

    @Test
    void shouldParseYearMonth() {
        assertEquals(date(2015, 3, 1), parse("201503"));
        assertEquals(date(2015, 3, 1), parse("2015-03"));
        assertEquals(date(2015, 3, 1), parse("+2015-03"));
        assertCannotParse("2018-00");
        assertCannotParse("2018-13");
        assertCannotParse("2015-3"); // Ambiguous if this should be interpreted as a year+month or ordinal date
    }

    @Test
    void shouldGetCorrectGqlStatus() {
        String badDate = "2015-3";
        assertCorrectGqlDescr(
                badDate,
                String.format(
                        "error: data exception - invalid date format. Cannot parse %s as a DATE. Calendar dates need to be specified using the format 'YYYY-MM', while ordinal dates need to be specified using the format 'YYYY-DDD'.",
                        GqlParams.StringParam.input.process(badDate)));
    }

    @Test
    void shouldParseYearWeek() {
        assertEquals(weekDate(2015, 5, 1), parse("2015W05"));
        assertEquals(weekDate(2015, 53, 1), parse("2015W53")); // 2015 had 53 weeks
        assertEquals(weekDate(2015, 5, 1), parse("2015W5"));
        assertEquals(weekDate(2015, 5, 1), parse("2015-W05"));
        assertEquals(weekDate(2015, 5, 1), parse("2015-W5"));
        assertEquals(weekDate(2015, 5, 1), parse("+2015-W05"));
        assertEquals(weekDate(2015, 5, 1), parse("+2015-W05"));
        assertEquals(weekDate(2015, 5, 1), parse("+2015W05"));
    }

    @Test
    void shouldParseYearQuarter() {
        assumeTrue(DateValue.QUARTER_DATES);
        assertEquals(quarterDate(2017, 3, 1), parse("2017Q3"));
        assertEquals(quarterDate(2017, 3, 1), parse("2017-Q3"));
        assertEquals(quarterDate(2017, 3, 1), parse("+2017-Q3"));
        assertEquals(quarterDate(2017, 3, 1), parse("+2017Q3"));
        assertCannotParse("2015Q0");
        assertCannotParse("2015Q5");
    }

    @Test
    void shouldParseCalendarDate() {
        assertEquals(date(2016, 1, 27), parse("20160127"));
        assertEquals(date(2016, 1, 27), parse("2016-1-27"));
        assertEquals(date(2016, 1, 27), parse("+2016-01-27"));
        assertEquals(date(2016, 1, 27), parse("+2016-1-27"));
        assertCannotParse("2015-01-32");
        assertCannotParse("2015-01-00");
    }

    @Test
    void shouldParseWeekDate() {
        assertEquals(weekDate(2015, 5, 6), parse("2015W056"));
        assertCannotParse("+2015W056");
        assertEquals(weekDate(2015, 5, 6), parse("2015-W05-6"));
        assertEquals(weekDate(2015, 5, 6), parse("+2015-W05-6"));
        assertEquals(weekDate(2015, 5, 6), parse("2015-W5-6"));
        assertEquals(weekDate(2015, 5, 6), parse("+2015-W5-6"));
    }

    @Test
    void shouldParseQuarterDate() {
        assumeTrue(DateValue.QUARTER_DATES);
        assertEquals(quarterDate(2017, 3, 92), parse("2017Q392"));
        assertEquals(quarterDate(2017, 3, 92), parse("2017-Q3-92"));
        assertEquals(quarterDate(2017, 3, 92), parse("+2017-Q3-92"));
    }

    @Test
    void shouldParseOrdinalDate() {
        assertEquals(ordinalDate(2017, 3), parse("2017003"));
        assertCannotParse("20173");
        assertEquals(ordinalDate(2017, 3), parse("2017-003"));
        assertEquals(ordinalDate(2017, 3), parse("+2017-003"));
        assertCannotParse("2017-366");
    }

    @Test
    void shouldEnforceStrictWeekRanges() {
        LocalDate localDate = weekDate(2017, 52, 7).temporal();
        assertEquals(DayOfWeek.SUNDAY, localDate.getDayOfWeek(), "Sunday is the seventh day of the week.");
        assertEquals(52, localDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
        assertEquals(localDate, date(2017, 12, 31).temporal());
        InvalidArgumentException expected = assertThrows(
                InvalidArgumentException.class, () -> weekDate(2017, 53, 1), "2017 does not have 53 weeks.");
        assertEquals("Year 2017 does not contain 53 weeks.", expected.getMessage());
        assertEquals(date(2016, 1, 1), weekDate(2015, 53, 5));
    }

    @Test
    void shouldEnforceStrictQuarterRanges() {
        assertEquals(date(2017, 3, 31), quarterDate(2017, 1, 90));
        assertThrows(InvalidArgumentException.class, () -> quarterDate(2017, 1, 0));
        assertThrows(InvalidArgumentException.class, () -> quarterDate(2017, 2, 0));
        assertThrows(InvalidArgumentException.class, () -> quarterDate(2017, 3, 0));
        assertThrows(InvalidArgumentException.class, () -> quarterDate(2017, 4, 0));
        assertThrows(InvalidArgumentException.class, () -> quarterDate(2017, 4, 93));
        assertThrows(InvalidArgumentException.class, () -> quarterDate(2017, 3, 93));
        assertThrows(InvalidArgumentException.class, () -> quarterDate(2017, 2, 92));
        assertThrows(InvalidArgumentException.class, () -> quarterDate(2017, 1, 92));
        assertThrows(InvalidArgumentException.class, () -> quarterDate(2017, 1, 91));
        assertEquals(date(2016, 3, 31), quarterDate(2016, 1, 91));
        assertThrows(InvalidArgumentException.class, () -> quarterDate(2016, 1, 92));
    }

    @Test
    void shouldNotParseInvalidDates() {
        assertCannotParse("2015W54"); // no year should have more than 53 weeks (2015 had 53 weeks)
        assertThrows(InvalidArgumentException.class, () -> parse("2017W53")); // 2017 only has 52 weeks
    }

    @Test
    void shouldWriteDate() {
        // given
        for (DateValue value : new DateValue[] {
            date(2016, 2, 29), date(2017, 12, 22),
        }) {
            List<DateValue> values = new ArrayList<>(1);
            ValueWriter<RuntimeException> writer = new ThrowingValueWriter.AssertOnly() {
                @Override
                public void writeDate(LocalDate localDate) {
                    values.add(date(localDate));
                }
            };

            // when
            value.writeTo(writer);

            // then
            assertEquals(singletonList(value), values);
        }
    }

    @Test
    void shouldAddDurationToDates() {
        assertEquals(date(2018, 2, 1), date(2018, 1, 1).add(DurationValue.duration(1, 0, 900, 0)));
        assertEquals(date(2018, 2, 28), date(2018, 1, 31).add(DurationValue.duration(1, 0, 0, 0)));
        assertEquals(date(2018, 1, 28), date(2018, 2, 28).add(DurationValue.duration(-1, 0, 0, 0)));
    }

    @Test
    void shouldReuseInstanceInArithmetics() {
        final DateValue date = date(2018, 2, 1);
        assertSame(date, date.add(DurationValue.duration(0, 0, 0, 0)));
        assertSame(date, date.add(DurationValue.duration(0, 0, 1, 1)));
        assertSame(date, date.add(DurationValue.duration(-0, 0, 1, -1)));
    }

    @Test
    void shouldSubtractDurationFromDates() {
        assertEquals(date(2018, 1, 1), date(2018, 2, 1).sub(DurationValue.duration(1, 0, 900, 0)));
        assertEquals(date(2018, 1, 28), date(2018, 2, 28).sub(DurationValue.duration(1, 0, 0, 0)));
        assertEquals(date(2018, 2, 28), date(2018, 1, 31).sub(DurationValue.duration(-1, 0, 0, 0)));
    }

    @Test
    void shouldEqualItself() {
        assertEqual(date(2018, 1, 31), date(2018, 1, 31));
    }

    @Test
    void shouldNotEqualOther() {
        assertNotEqual(date(2018, 1, 31), date(2018, 1, 30));
    }

    private static void assertCannotParse(String text) {
        assertThrows(TemporalParseException.class, () -> parse(text), format("'%s' parsed to value", text));
    }

    private static void assertCorrectGqlDescr(String text, String expectedCauseGqlStatusDescription) {
        var e = assertThrows(Exception.class, () -> parse(text));
        try {
            parse(text);
        } catch (Exception ex) {
            if (e instanceof ErrorGqlStatusObject gso && gso.cause().isPresent()) {
                assertEquals(gso.cause().get().statusDescription(), expectedCauseGqlStatusDescription);
            } else {
                fail("Expected exception to have a gql-code with cause");
            }
        }
    }
}
