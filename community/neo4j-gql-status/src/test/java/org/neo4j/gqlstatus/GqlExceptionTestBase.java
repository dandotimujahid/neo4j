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
package org.neo4j.gqlstatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

abstract class GqlExceptionTestBase {

    abstract <T extends Exception & ErrorGqlStatusObject> T testException(
            ErrorGqlStatusObject innerObject, String message);

    abstract <T extends Exception & ErrorGqlStatusObject> T testException(
            ErrorGqlStatusObject innerObject, String message, Throwable cause);

    @Test
    void shouldNotConvertJavaExceptionToCause() {
        ErrorGqlStatusObject gqlObject = ErrorGqlStatusObjectImplementation.from(GqlStatusInfoCodes.STATUS_25N11)
                .build();
        Throwable cause = new RuntimeException();
        ErrorGqlStatusObject errorWithJavaExceptionCause = testException(gqlObject, "message", cause);
        assertEquals(Optional.empty(), errorWithJavaExceptionCause.cause());
    }

    @Test
    void shouldConvertGqlExceptionToCause() {
        ErrorGqlStatusObject gqlObject = ErrorGqlStatusObjectImplementation.from(GqlStatusInfoCodes.STATUS_25N11)
                .build();
        ErrorGqlStatusObject causeGqlObject = ErrorGqlStatusObjectImplementation.from(GqlStatusInfoCodes.STATUS_22N08)
                .withParam(GqlParams.StringParam.option1, "myOption")
                .withParam(GqlParams.StringParam.option2, "yourOption")
                .build();
        Throwable cause = testException(causeGqlObject, "inner message");
        ErrorGqlStatusObject errorWithGqlExceptionCause = testException(gqlObject, "message", cause);
        assertTrue(errorWithGqlExceptionCause.cause().isPresent());
        ErrorGqlStatusObject firstCause = errorWithGqlExceptionCause.cause().get();
        assertEquals(GqlStatusInfoCodes.STATUS_22N08.getStatusString(), firstCause.gqlStatus());
        assertThat(firstCause.statusDescription()).contains("cannot combine 'myOption' with 'yourOption'");
        assertFalse(firstCause.cause().isPresent());
    }

    @Test
    void shouldConvertNotYetPortedGqlExceptionToStandardCause() {
        ErrorGqlStatusObject gqlObject = ErrorGqlStatusObjectImplementation.from(GqlStatusInfoCodes.STATUS_25N11)
                .build();
        Throwable cause = testException(null, "inner message");
        ErrorGqlStatusObject errorWithGqlExceptionCause = testException(gqlObject, "message", cause);
        assertTrue(errorWithGqlExceptionCause.cause().isPresent());
        ErrorGqlStatusObject firstCause = errorWithGqlExceptionCause.cause().get();
        assertEquals(GqlStatusInfoCodes.STATUS_50N42.getStatusString(), firstCause.gqlStatus());
        assertFalse(firstCause.cause().isPresent());
    }

    @Test
    void shouldAppendGqlExceptionToExistingGqlCause() {
        ErrorGqlStatusObject existingCauseGqlObject = ErrorGqlStatusObjectImplementation.from(
                        GqlStatusInfoCodes.STATUS_22N06)
                .withParam(GqlParams.StringParam.option, "option")
                .atPosition(2, 3, 7)
                .build();

        ErrorGqlStatusObject gqlObject = ErrorGqlStatusObjectImplementation.from(GqlStatusInfoCodes.STATUS_25N11)
                .withCause(existingCauseGqlObject)
                .build();

        ErrorGqlStatusObject exceptionCauseGqlObject = ErrorGqlStatusObjectImplementation.from(
                        GqlStatusInfoCodes.STATUS_22N08)
                .withParam(GqlParams.StringParam.option1, "myOption")
                .withParam(GqlParams.StringParam.option2, "yourOption")
                .build();

        Throwable cause = testException(exceptionCauseGqlObject, "inner message");
        ErrorGqlStatusObject errorWithGqlExceptionCause = testException(gqlObject, "message", cause);

        assertTrue(errorWithGqlExceptionCause.cause().isPresent());
        ErrorGqlStatusObject firstCause = errorWithGqlExceptionCause.cause().get();
        assertEquals(GqlStatusInfoCodes.STATUS_22N06.getStatusString(), firstCause.gqlStatus());
        assertThat(firstCause.statusDescription()).contains("'option' needs to be specified");
        Object position = firstCause.diagnosticRecord().get("_position");
        assertEquals(Map.of("line", 2, "column", 3, "offset", 7), position);

        assertTrue(firstCause.cause().isPresent());
        ErrorGqlStatusObject secondCause = firstCause.cause().get();
        assertEquals(GqlStatusInfoCodes.STATUS_22N08.getStatusString(), secondCause.gqlStatus());
        assertThat(secondCause.statusDescription()).contains("cannot combine 'myOption' with 'yourOption'");
        assertFalse(secondCause.cause().isPresent());
    }

    @Test
    void shouldNotAppendGqlExceptionToExistingGqlCausesIfAlreadyPresent() {
        ErrorGqlStatusObject causeGqlObject = ErrorGqlStatusObjectImplementation.from(GqlStatusInfoCodes.STATUS_22N06)
                .withParam(GqlParams.StringParam.option, "option")
                .withCause(ErrorGqlStatusObjectImplementation.from(GqlStatusInfoCodes.STATUS_22N08)
                        .withParam(GqlParams.StringParam.option1, "myOption")
                        .withParam(GqlParams.StringParam.option2, "yourOption")
                        .build())
                .build();

        var cause = testException(causeGqlObject, "inner message");

        ErrorGqlStatusObject gqlObject = ErrorGqlStatusObjectImplementation.from(GqlStatusInfoCodes.STATUS_25N11)
                .withCause(cause)
                .build();

        ErrorGqlStatusObject errorWithGqlExceptionCause = testException(gqlObject, "message", cause);

        assertTrue(errorWithGqlExceptionCause.cause().isPresent());
        ErrorGqlStatusObject firstCause = errorWithGqlExceptionCause.cause().get();
        assertEquals(GqlStatusInfoCodes.STATUS_22N06.getStatusString(), firstCause.gqlStatus());
        assertThat(firstCause.statusDescription()).contains("'option' needs to be specified");

        assertTrue(firstCause.cause().isPresent());
        ErrorGqlStatusObject secondCause = firstCause.cause().get();
        assertEquals(GqlStatusInfoCodes.STATUS_22N08.getStatusString(), secondCause.gqlStatus());
        assertThat(secondCause.statusDescription()).contains("cannot combine 'myOption' with 'yourOption'");

        assertTrue(secondCause.cause().isEmpty());
    }

    @Test
    void shouldAppendGqlExceptionToExistingGqlCausesIfAlmostSameCauseAlreadyPresent() {
        ErrorGqlStatusObject causeGqlObject = ErrorGqlStatusObjectImplementation.from(GqlStatusInfoCodes.STATUS_22N06)
                .withParam(GqlParams.StringParam.option, "option")
                .withCause(ErrorGqlStatusObjectImplementation.from(GqlStatusInfoCodes.STATUS_22N08)
                        .withParam(GqlParams.StringParam.option1, "myOption")
                        .withParam(GqlParams.StringParam.option2, "yourOption")
                        .build())
                .build();

        var cause = testException(causeGqlObject, "inner message");

        ErrorGqlStatusObject gqlObject = ErrorGqlStatusObjectImplementation.from(GqlStatusInfoCodes.STATUS_25N11)
                .withCause(cause)
                .build();

        ErrorGqlStatusObject almostIdenticalExceptionCauseGqlObject = ErrorGqlStatusObjectImplementation.from(
                        GqlStatusInfoCodes.STATUS_22N06)
                .withParam(GqlParams.StringParam.option, "option")
                .withCause(ErrorGqlStatusObjectImplementation.from(GqlStatusInfoCodes.STATUS_22N08)
                        .withParam(GqlParams.StringParam.option1, "myOption")
                        .withParam(GqlParams.StringParam.option2, "your-Option")
                        .build())
                .build();

        Throwable almostIdenticalCause = testException(almostIdenticalExceptionCauseGqlObject, "inner message");
        ErrorGqlStatusObject errorWithGqlExceptionCause = testException(gqlObject, "message", almostIdenticalCause);

        assertTrue(errorWithGqlExceptionCause.cause().isPresent());
        ErrorGqlStatusObject cause1 = errorWithGqlExceptionCause.cause().get();
        assertEquals(GqlStatusInfoCodes.STATUS_22N06.getStatusString(), cause1.gqlStatus());
        assertThat(cause1.statusDescription()).contains("'option' needs to be specified");

        assertTrue(cause1.cause().isPresent());
        ErrorGqlStatusObject cause2 = cause1.cause().get();
        assertEquals(GqlStatusInfoCodes.STATUS_22N08.getStatusString(), cause2.gqlStatus());
        assertThat(cause2.statusDescription()).contains("cannot combine 'myOption' with 'yourOption'");

        assertTrue(cause2.cause().isPresent());
        ErrorGqlStatusObject cause3 = cause2.cause().get();
        assertEquals(GqlStatusInfoCodes.STATUS_22N06.getStatusString(), cause3.gqlStatus());
        assertThat(cause3.statusDescription()).contains("'option' needs to be specified");

        assertTrue(cause3.cause().isPresent());
        ErrorGqlStatusObject cause4 = cause3.cause().get();
        assertEquals(GqlStatusInfoCodes.STATUS_22N08.getStatusString(), cause4.gqlStatus());
        assertThat(cause4.statusDescription()).contains("cannot combine 'myOption' with 'your-Option'");

        assertTrue(cause4.cause().isEmpty());
    }
}
