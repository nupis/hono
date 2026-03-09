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

import org.eclipse.hono.service.metric.Metrics;
import org.eclipse.hono.service.metric.NoopBasedMetrics;

/**
 * Metrics for the EDC adapter.
 */
public interface EdcAdapterMetrics extends Metrics {

    /**
     * The no-op implementation.
     */
    EdcAdapterMetrics NOOP = new Noop();

    /**
     * Records that a polling cycle has completed.
     */
    void recordCycleCompleted();

    /**
     * Records a successful contract negotiation.
     */
    void recordNegotiationSuccess();

    /**
     * Records a failed contract negotiation.
     */
    void recordNegotiationFailure();

    /**
     * Records a successful data transfer.
     */
    void recordTransferSuccess();

    /**
     * Records a failed data transfer.
     */
    void recordTransferFailure();

    /**
     * Records that a telemetry message has been forwarded to Hono.
     */
    void recordTelemetryForwarded();

    /**
     * Records the latency of a single device fetch operation.
     *
     * @param duration The duration of the fetch.
     */
    void recordFetchLatency(Duration duration);

    /**
     * Updates the timestamp of the last successful fetch.
     */
    void updateLastSuccessfulFetch();

    /**
     * Gets the epoch seconds of the last successful fetch.
     *
     * @return The timestamp in epoch seconds, or 0 if no successful fetch has occurred.
     */
    long getLastSuccessfulFetchTimestamp();

    /**
     * A no-op implementation for this specific metrics type.
     */
    final class Noop extends NoopBasedMetrics implements EdcAdapterMetrics {

        private Noop() {
        }

        @Override
        public void recordCycleCompleted() {
        }

        @Override
        public void recordNegotiationSuccess() {
        }

        @Override
        public void recordNegotiationFailure() {
        }

        @Override
        public void recordTransferSuccess() {
        }

        @Override
        public void recordTransferFailure() {
        }

        @Override
        public void recordTelemetryForwarded() {
        }

        @Override
        public void recordFetchLatency(final Duration duration) {
        }

        @Override
        public void updateLastSuccessfulFetch() {
        }

        @Override
        public long getLastSuccessfulFetchTimestamp() {
            return 0;
        }
    }
}
