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
package org.neo4j.bolt.testing.assertions;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.assertj.core.api.AbstractAssert;
import org.neo4j.bolt.negotiation.ProtocolVersion;
import org.neo4j.bolt.testing.client.BoltTestConnection;
import org.neo4j.bolt.testing.client.error.BoltTestClientIOException;
import org.neo4j.bolt.testing.client.error.BoltTestClientInterruptedException;
import org.neo4j.bolt.testing.client.struct.ProtocolProposal;
import org.neo4j.function.Predicates;

public abstract class BoltTestConnectionAssertions<
                SELF extends BoltTestConnectionAssertions<SELF, ACTUAL>, ACTUAL extends BoltTestConnection>
        extends AbstractAssert<SELF, ACTUAL> {

    protected BoltTestConnectionAssertions(ACTUAL connection, Class<SELF> selfType) {
        super(connection, selfType);
    }

    @SuppressWarnings("unchecked")
    public SELF negotiates(ProtocolVersion expected) {
        this.isNotNull();

        try {
            var actual = this.actual.receiveNegotiatedVersion();
            if (!expected.matches(actual)) {
                failWithActualExpectedAndMessage(
                        actual,
                        expected,
                        "Expected connection to negotiate version <%s> but got <%s>",
                        expected,
                        actual);
            }
        } catch (BoltTestClientIOException ex) {
            throw new AssertionError("Failed to receive expected negotiation response", ex);
        } catch (BoltTestClientInterruptedException ex) {
            throw new AssertionError("Interrupted while awaiting negotiation response", ex);
        }

        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    public SELF receivesProtocolProposal(Consumer<ProtocolProposal> assertions) {
        this.isNotNull();

        try {
            var actual = this.actual.receiveProtocolProposal();
            assertions.accept(actual);
        } catch (BoltTestClientIOException ex) {
            throw new AssertionError("Failed to receive valid protocol proposal", ex);
        } catch (BoltTestClientInterruptedException ex) {
            throw new AssertionError("Interrupted while awaiting protocol proposal", ex);
        }

        return (SELF) this;
    }

    public SELF receivesProtocolProposal() {
        return this.receivesProtocolProposal(proposal -> {});
    }

    public SELF negotiatesDefaultVersion() {
        return this.negotiates(BoltTestConnection.DEFAULT_PROTOCOL_VERSION);
    }

    @SuppressWarnings("unchecked")
    public SELF failsToNegotiateVersion() {
        this.isNotNull();

        try {
            var actual = this.actual.receiveNegotiatedVersion();

            if (!ProtocolVersion.INVALID.equals(actual)) {
                failWithActualExpectedAndMessage(
                        actual,
                        ProtocolVersion.INVALID,
                        "Expected connection to fail version negotiation but got <%s>",
                        actual);
            }
        } catch (BoltTestClientIOException ex) {
            throw new AssertionError("Failed to receive expected negotiation response", ex);
        } catch (BoltTestClientInterruptedException ex) {
            throw new AssertionError("Interrupted while awaiting negotiation response", ex);
        }

        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    public SELF isEventuallyTerminated() {
        try {
            Predicates.await(
                    () -> {
                        try {
                            return actual.isClosed();
                        } catch (BoltTestClientInterruptedException ex) {
                            fail(ex);
                        }
                        return false;
                    },
                    5,
                    TimeUnit.MINUTES);
        } catch (TimeoutException ex) {
            fail("Connection failed to terminate within 5 minutes", ex);
        }

        return (SELF) this;
    }
}
