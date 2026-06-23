/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hono.adapter.edc;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.hono.service.metric.NoopBasedMetrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Micrometer-based implementation of {@link EdcAdapterMetrics}.
 * <p>
 * Extends {@link NoopBasedMetrics} to provide no-op implementations for the base
 * {@link org.eclipse.hono.service.metric.Metrics} interface methods that are not
 * relevant to the EDC polling adapter (e.g. connection tracking, command reporting).
 */
public class MicrometerBasedEdcAdapterMetrics extends NoopBasedMetrics implements EdcAdapterMetrics {

    private final Counter cyclesTotal;
    private final Counter negotiationsSuccess;
    private final Counter negotiationsFailure;
    private final Counter transfersSuccess;
    private final Counter transfersFailure;
    private final Counter telemetryForwarded;
    private final Timer fetchLatency;
    private final AtomicLong lastSuccessfulFetchTimestamp;

    /**
     * Creates a new metrics instance.
     *
     * @param registry The meter registry.
     * @throws NullPointerException if registry is {@code null}.
     */
    public MicrometerBasedEdcAdapterMetrics(final MeterRegistry registry) {
        Objects.requireNonNull(registry);

        this.cyclesTotal = Counter.builder("edc.adapter.cycles.total")
                .description("Total number of completed polling cycles")
                .register(registry);
        this.negotiationsSuccess = Counter.builder("edc.adapter.negotiations.total")
                .tag("outcome", "success")
                .description("Total contract negotiations")
                .register(registry);
        this.negotiationsFailure = Counter.builder("edc.adapter.negotiations.total")
                .tag("outcome", "failure")
                .description("Total contract negotiations")
                .register(registry);
        this.transfersSuccess = Counter.builder("edc.adapter.transfers.total")
                .tag("outcome", "success")
                .description("Total data transfers")
                .register(registry);
        this.transfersFailure = Counter.builder("edc.adapter.transfers.total")
                .tag("outcome", "failure")
                .description("Total data transfers")
                .register(registry);
        this.telemetryForwarded = Counter.builder("edc.adapter.telemetry.forwarded")
                .description("Total telemetry messages forwarded to Hono")
                .register(registry);
        this.fetchLatency = Timer.builder("edc.adapter.fetch.latency")
                .description("Latency of single device fetch operations")
                .register(registry);
        this.lastSuccessfulFetchTimestamp = new AtomicLong(0);
        registry.gauge("edc.adapter.last_successful_fetch_epoch_seconds",
                lastSuccessfulFetchTimestamp,
                AtomicLong::doubleValue);
    }

    @Override
    public void recordCycleCompleted() {
        cyclesTotal.increment();
    }

    @Override
    public void recordNegotiationSuccess() {
        negotiationsSuccess.increment();
    }

    @Override
    public void recordNegotiationFailure() {
        negotiationsFailure.increment();
    }

    @Override
    public void recordTransferSuccess() {
        transfersSuccess.increment();
    }

    @Override
    public void recordTransferFailure() {
        transfersFailure.increment();
    }

    @Override
    public void recordTelemetryForwarded() {
        telemetryForwarded.increment();
    }

    @Override
    public void recordFetchLatency(final Duration duration) {
        fetchLatency.record(duration);
    }

    @Override
    public void updateLastSuccessfulFetch() {
        lastSuccessfulFetchTimestamp.set(System.currentTimeMillis() / 1000L);
    }

    @Override
    public long getLastSuccessfulFetchTimestamp() {
        return lastSuccessfulFetchTimestamp.get();
    }
}
