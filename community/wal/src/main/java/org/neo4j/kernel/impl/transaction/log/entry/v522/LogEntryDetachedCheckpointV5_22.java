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
package org.neo4j.kernel.impl.transaction.log.entry.v522;

import static org.neo4j.kernel.impl.transaction.log.entry.LogEntryTypeCodes.DETACHED_CHECK_POINT_V5_0;

import java.util.Objects;
import org.neo4j.kernel.KernelVersion;
import org.neo4j.kernel.impl.transaction.log.LogPosition;
import org.neo4j.kernel.impl.transaction.log.entry.AbstractVersionAwareLogEntry;
import org.neo4j.storageengine.api.StoreId;
import org.neo4j.storageengine.api.TransactionId;
import org.neo4j.string.Mask;

public class LogEntryDetachedCheckpointV5_22 extends AbstractVersionAwareLogEntry {
    private final TransactionId transactionId;
    private final long lastAppendIndex;
    private final LogPosition oldestNotCompletedPosition;
    private final LogPosition checkpointedLogPosition;
    private final long checkpointTime;
    private final StoreId storeId;
    private final String reason;
    private final boolean consensusIndexInCheckpoint;

    public LogEntryDetachedCheckpointV5_22(
            KernelVersion kernelVersion,
            TransactionId transactionId,
            long lastAppendIndex,
            LogPosition oldestNotCompletedPosition,
            LogPosition checkpointedLogPosition,
            long checkpointMillis,
            StoreId storeId,
            String reason) {
        super(kernelVersion, DETACHED_CHECK_POINT_V5_0);
        this.transactionId = transactionId;
        this.oldestNotCompletedPosition = oldestNotCompletedPosition;
        this.checkpointedLogPosition = checkpointedLogPosition;
        this.checkpointTime = checkpointMillis;
        this.storeId = storeId;
        this.lastAppendIndex = lastAppendIndex;
        this.reason = reason;
        this.consensusIndexInCheckpoint = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LogEntryDetachedCheckpointV5_22 that = (LogEntryDetachedCheckpointV5_22) o;
        return lastAppendIndex == that.lastAppendIndex
                && checkpointTime == that.checkpointTime
                && consensusIndexInCheckpoint == that.consensusIndexInCheckpoint
                && Objects.equals(transactionId, that.transactionId)
                && Objects.equals(oldestNotCompletedPosition, that.oldestNotCompletedPosition)
                && Objects.equals(checkpointedLogPosition, that.checkpointedLogPosition)
                && Objects.equals(storeId, that.storeId)
                && Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                transactionId,
                lastAppendIndex,
                oldestNotCompletedPosition,
                checkpointedLogPosition,
                checkpointTime,
                storeId,
                reason,
                consensusIndexInCheckpoint);
    }

    public StoreId getStoreId() {
        return storeId;
    }

    public LogPosition getCheckpointedLogPosition() {
        return checkpointedLogPosition;
    }

    public LogPosition getOldestNotCompletedPosition() {
        return oldestNotCompletedPosition;
    }

    public TransactionId getTransactionId() {
        return transactionId;
    }

    public String getReason() {
        return reason;
    }

    public long getCheckpointTime() {
        return checkpointTime;
    }

    public boolean consensusIndexInCheckpoint() {
        return consensusIndexInCheckpoint;
    }

    public long getLastAppendIndex() {
        return lastAppendIndex;
    }

    @Override
    public String toString(Mask mask) {
        return "LogEntryDetachedCheckpointV5_22{" + "transactionId=" + transactionId + ", lastAppendIndex="
                + lastAppendIndex + ", oldestNotCompletedPosition="
                + oldestNotCompletedPosition + ", checkpointedLogPosition=" + checkpointedLogPosition
                + ", checkpointTime=" + checkpointTime
                + ", storeId=" + storeId + ", reason='" + reason + '\'' + ", consensusIndexInCheckpoint="
                + consensusIndexInCheckpoint + '}';
    }
}
