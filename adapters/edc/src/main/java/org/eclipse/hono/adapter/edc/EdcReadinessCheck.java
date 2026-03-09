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

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * SmallRye Health readiness check that verifies EDC Consumer Management API connectivity.
 * <p>
 * Reports UP when the EDC Management API is reachable (catalog query succeeds),
 * DOWN when it is unreachable or returns errors.
 * Includes the last successful fetch timestamp in the response data.
 */
@Readiness
@ApplicationScoped
public class EdcReadinessCheck implements HealthCheck {

    private static final Logger LOG = LoggerFactory.getLogger(EdcReadinessCheck.class);
    private static final String CHECK_NAME = "EDC Consumer connectivity";

    private volatile boolean edcReachable;
    private volatile String lastError;

    /**
     * Creates a new readiness check.
     */
    public EdcReadinessCheck() {
        this.edcReachable = false;
    }

    /**
     * Marks the EDC Consumer as reachable.
     */
    public void markReachable() {
        this.edcReachable = true;
        this.lastError = null;
    }

    /**
     * Marks the EDC Consumer as unreachable.
     *
     * @param error A description of the connectivity failure.
     */
    public void markUnreachable(final String error) {
        this.edcReachable = false;
        this.lastError = error;
        LOG.warn("EDC Consumer unreachable: {}", error);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Reports UP when the EDC Management API was reachable during the last polling cycle,
     * DOWN otherwise. Includes error details when down.
     */
    @Override
    public HealthCheckResponse call() {
        final HealthCheckResponseBuilder builder = HealthCheckResponse.named(CHECK_NAME);

        if (edcReachable) {
            builder.up();
        } else {
            builder.down();
            if (lastError != null) {
                builder.withData("error", lastError);
            }
        }

        return builder.build();
    }
}
