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
package org.neo4j.internal.batchimport.staging;

import static java.lang.String.format;
import static java.lang.System.nanoTime;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.neo4j.batchimport.api.Configuration;
import org.neo4j.internal.batchimport.executor.ParkStrategy;
import org.neo4j.internal.batchimport.stats.ProcessingStats;
import org.neo4j.internal.batchimport.stats.StatsProvider;
import org.neo4j.internal.batchimport.stats.StepStats;
import org.neo4j.util.concurrent.WorkSync;

/**
 * Basic implementation of a {@link Step}. Does the most plumbing job of building a step implementation.
 */
public abstract class AbstractStep<T> implements Step<T> {
    public static final ParkStrategy PARK = new ParkStrategy.Park(IS_OS_WINDOWS ? 10_000 : 500, MICROSECONDS);

    protected final StageControl control;
    private final String name;

    @SuppressWarnings("rawtypes")
    protected volatile Step downstream;

    protected volatile WorkSync<Downstream, SendDownstream> downstreamWorkSync;
    private volatile boolean endOfUpstream;
    protected volatile Throwable panic;
    private final CountDownLatch completed = new CountDownLatch(1);
    protected int orderingGuarantees;

    // Milliseconds awaiting downstream to process batches so that its queue size goes beyond the configured threshold
    // If this is big then it means that this step is faster than downstream.
    protected final LongAdder downstreamIdleTime = new LongAdder();
    // Milliseconds awaiting upstream to hand over batches to this step.
    // If this is big then it means that this step is faster than upstream.
    protected final LongAdder upstreamIdleTime = new LongAdder();
    // Number of batches received, but not yet processed.
    protected final AtomicInteger queuedBatches = new AtomicInteger();
    // Number of batches fully processed
    protected final AtomicLong doneBatches = new AtomicLong();
    // Milliseconds spent processing all received batches.
    protected final MovingAverage totalProcessingTime;
    protected long startTime;
    protected long endTime;
    protected final List<StatsProvider> additionalStatsProvider;
    protected final Configuration config;

    public AbstractStep(
            StageControl control, String name, Configuration config, StatsProvider... additionalStatsProvider) {
        this.control = control;
        this.name = name;
        this.config = config;
        this.totalProcessingTime = new MovingAverage(config.movingAverageSize());
        this.additionalStatsProvider = asList(additionalStatsProvider);
    }

    @Override
    public void start(int orderingGuarantees) {
        this.orderingGuarantees = orderingGuarantees;
        resetStats();
    }

    protected boolean guarantees(int orderingGuaranteeFlag) {
        return (orderingGuarantees & orderingGuaranteeFlag) != 0;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void receivePanic(Throwable cause) {
        this.panic = cause;
    }

    protected boolean stillWorking() {
        if (isPanic()) { // There has been a panic, so we'll just stop working
            return false;
        }

        return !endOfUpstream || queuedBatches.get() != 0;
    }

    protected boolean isPanic() {
        return panic != null;
    }

    @Override
    public boolean isCompleted() {
        return completed.getCount() == 0;
    }

    @Override
    public boolean awaitCompleted(long time, TimeUnit unit) throws InterruptedException {
        return completed.await(time, unit);
    }

    protected void issuePanic(Throwable cause) {
        issuePanic(cause, true);
    }

    protected void issuePanic(Throwable cause, boolean rethrow) {
        control.panic(cause);
        if (rethrow) {
            throw new RuntimeException(cause);
        }
    }

    protected void assertHealthy() {
        if (isPanic()) {
            throw new RuntimeException(panic);
        }
    }

    @Override
    public void setDownstream(Step<?> downstream) {
        assert downstream != this;
        this.downstream = downstream;
        //noinspection unchecked
        this.downstreamWorkSync = new WorkSync<>(new Downstream((Step<Object>) downstream, doneBatches));
    }

    @Override
    public StepStats stats() {
        Collection<StatsProvider> providers = new ArrayList<>();
        collectStatsProviders(providers);
        return new StepStats(name, stillWorking(), providers);
    }

    protected void collectStatsProviders(Collection<StatsProvider> into) {
        long done = doneBatches.get();
        int numProcessors = processors(0);
        long processingTimeTotal = totalProcessingTime.total();
        into.add(new ProcessingStats(
                done + queuedBatches.get(),
                done,
                processingTimeTotal,
                totalProcessingTime.average() / numProcessors,
                processingTimeTotal / numProcessors,
                upstreamIdleTime.sum(),
                downstreamIdleTime.sum()));
        into.addAll(additionalStatsProvider);
    }

    @Override
    public boolean isIdle() {
        // queuedBatches is increment on receiving a batch, decremented after completing a batch
        return queuedBatches.get() == 0;
    }

    @Override
    public void endOfUpstream() {
        endOfUpstream = true;
        checkNotifyEndDownstream();
    }

    protected void checkNotifyEndDownstream() {
        if (!stillWorking() && !isCompleted()) {
            synchronized (this) {
                // Only allow a single thread to notify that we've ended our stream as well as calling done()
                // stillWorking(), once false cannot again return true so no need to check
                if (!isCompleted()) {
                    // In the event of panic do not even try to do any sort of completion step, which btw may entail
                    // sending more batches downstream
                    // or do heavy end-result calculations
                    if (!isPanic()) {
                        done();
                    }
                    if (downstream != null) {
                        downstream.endOfUpstream();
                    }
                    endTime = nanoTime();
                    completed.countDown();
                }
            }
        }
    }

    /**
     * Called once, when upstream has run out of batches to send and all received batches have been
     * processed successfully.
     */
    protected void done() {}

    @Override
    public void close() throws Exception { // Do nothing by default
    }

    protected void resetStats() {
        downstreamIdleTime.reset();
        upstreamIdleTime.reset();
        queuedBatches.set(0);
        doneBatches.set(0);
        totalProcessingTime.reset();
        startTime = nanoTime();
        endTime = 0;
    }

    @Override
    public String toString() {
        return format(
                "%s[%s, processors:%d, batches:%d", getClass().getSimpleName(), name, processors(0), doneBatches.get());
    }
}
