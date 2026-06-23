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
package org.eclipse.hono.adapter.edc.client.model;

import java.util.Objects;

/**
 * Fetched telemetry data from an EDC data plane, ready to forward into Hono.
 *
 * @param deviceId The Hono device ID (= EDC asset ID).
 * @param tenantId The Hono tenant ID.
 * @param contentType The MIME type of the telemetry payload.
 * @param payload The raw telemetry data from the EDC data plane.
 * @param correlationId The correlation ID linking EDC transfer to Hono message.
 */
public record DeviceTelemetryResult(
        String deviceId,
        String tenantId,
        String contentType,
        byte[] payload,
        String correlationId) {

    /**
     * Creates a DeviceTelemetryResult with validation.
     *
     * @param deviceId The Hono device ID.
     * @param tenantId The Hono tenant ID.
     * @param contentType The MIME type of the telemetry payload.
     * @param payload The raw telemetry data.
     * @param correlationId The correlation ID.
     */
    public DeviceTelemetryResult {
        Objects.requireNonNull(deviceId, "deviceId must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(contentType, "contentType must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
    }
}
