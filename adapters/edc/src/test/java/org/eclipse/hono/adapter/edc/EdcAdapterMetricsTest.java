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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Tests for {@link EdcAdapterMetrics} and {@link MicrometerBasedEdcAdapterMetrics}.
 */
class EdcAdapterMetricsTest {

    @Test
    void testNoopInstanceExists() {
        assertNotNull(EdcAdapterMetrics.NOOP);
    }

    @Test
    void testNoopImplementsInterface() {
        final EdcAdapterMetrics metrics = EdcAdapterMetrics.NOOP;
        // NOOP should not throw on any interface method calls
        metrics.recordCycleCompleted();
        metrics.recordNegotiationSuccess();
        metrics.recordNegotiationFailure();
        metrics.recordTransferSuccess();
        metrics.recordTransferFailure();
        metrics.recordTelemetryForwarded();
        metrics.recordFetchLatency(Duration.ofMillis(100));
        metrics.updateLastSuccessfulFetch();
        assertNotNull(metrics);
    }

    @Test
    void testNoopLastSuccessfulFetchReturnsZero() {
        assertEquals(0, EdcAdapterMetrics.NOOP.getLastSuccessfulFetchTimestamp());
    }

    @Test
    void testMicrometerLastSuccessfulFetchGaugeRegistered() {
        final var registry = new SimpleMeterRegistry();
        new MicrometerBasedEdcAdapterMetrics(registry);

        final Gauge gauge = registry.find("edc.adapter.last_successful_fetch_epoch_seconds").gauge();
        assertNotNull(gauge, "last_successful_fetch_epoch_seconds gauge should be registered");
        assertEquals(0.0, gauge.value(), "initial value should be 0");
    }

    @Test
    void testMicrometerLastSuccessfulFetchUpdatesGauge() {
        final var registry = new SimpleMeterRegistry();
        final var metrics = new MicrometerBasedEdcAdapterMetrics(registry);

        metrics.updateLastSuccessfulFetch();

        final Gauge gauge = registry.find("edc.adapter.last_successful_fetch_epoch_seconds").gauge();
        assertNotNull(gauge);
        assertTrue(gauge.value() > 0, "gauge should be non-zero after update");

        final long epochSeconds = (long) gauge.value();
        final long nowEpochSeconds = System.currentTimeMillis() / 1000L;
        assertTrue(Math.abs(nowEpochSeconds - epochSeconds) < 5,
                "gauge should report current time in epoch seconds");
    }

    @Test
    void testMicrometerGetLastSuccessfulFetchTimestampReturnsEpochSeconds() {
        final var registry = new SimpleMeterRegistry();
        final var metrics = new MicrometerBasedEdcAdapterMetrics(registry);

        assertEquals(0, metrics.getLastSuccessfulFetchTimestamp());

        metrics.updateLastSuccessfulFetch();

        final long timestamp = metrics.getLastSuccessfulFetchTimestamp();
        final long nowEpochSeconds = System.currentTimeMillis() / 1000L;
        assertTrue(timestamp > 0, "timestamp should be non-zero after update");
        assertTrue(Math.abs(nowEpochSeconds - timestamp) < 5,
                "timestamp should be in epoch seconds");
    }
}
