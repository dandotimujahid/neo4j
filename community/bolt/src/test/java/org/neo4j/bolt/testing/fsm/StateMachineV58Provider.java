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
package org.neo4j.bolt.testing.fsm;

import org.neo4j.bolt.negotiation.ProtocolVersion;
import org.neo4j.bolt.protocol.common.BoltProtocol;
import org.neo4j.bolt.protocol.v58.BoltProtocolV58;
import org.neo4j.bolt.testing.messages.BoltMessages;
import org.neo4j.bolt.testing.messages.BoltV58Messages;

public class StateMachineV58Provider implements StateMachineProvider {
    private static final StateMachineProvider INSTANCE = new StateMachineV58Provider();

    private StateMachineV58Provider() {}

    public static StateMachineProvider getInstance() {
        return INSTANCE;
    }

    @Override
    public ProtocolVersion version() {
        return BoltProtocolV58.VERSION;
    }

    @Override
    public BoltMessages messages() {
        return BoltV58Messages.getInstance();
    }

    @Override
    public BoltProtocol protocol() {
        return BoltProtocolV58.getInstance();
    }
}
