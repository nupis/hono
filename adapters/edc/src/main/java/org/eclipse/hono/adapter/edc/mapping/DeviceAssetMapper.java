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
package org.eclipse.hono.adapter.edc.mapping;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.hono.client.registry.DeviceRegistrationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentracing.SpanContext;
import io.vertx.core.Future;

/**
 * Maps EDC dataspace asset IDs to Hono device identities.
 * <p>
 * Asset ID = Device ID (direct 1:1 mapping). The tenant is provided by the
 * caller (the polling service iterates tenants). This mapper validates the
 * device exists in the given tenant via the Device Registration API.
 */
public class DeviceAssetMapper {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceAssetMapper.class);

    private final DeviceRegistrationClient registrationClient;

    /**
     * Creates a new mapper.
     *
     * @param registrationClient The client for the Device Registration API.
     * @throws NullPointerException if registrationClient is {@code null}.
     */
    public DeviceAssetMapper(final DeviceRegistrationClient registrationClient) {
        this.registrationClient = Objects.requireNonNull(registrationClient);
    }

    /**
     * Resolves an EDC asset ID to a Hono device identity within a given tenant.
     *
     * @param tenantId The tenant to look up the device in.
     * @param assetId The EDC asset ID (= Hono device ID).
     * @param spanContext The tracing span context, or {@code null}.
     * @return A future containing an {@link Optional} with the resolved device mapping,
     *         or {@link Optional#empty()} if the device is not found or an error occurs.
     * @throws NullPointerException if tenantId or assetId is {@code null}.
     */
    public Future<Optional<DeviceMapping>> resolveDevice(
            final String tenantId,
            final String assetId,
            final SpanContext spanContext) {

        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(assetId);

        return registrationClient.assertRegistration(tenantId, assetId, null, spanContext)
                .map(assertion -> {
                    LOG.debug("resolved asset [{}] to device [tenant: {}, device: {}]",
                            assetId, tenantId, assetId);
                    return Optional.of(new DeviceMapping(tenantId, assetId));
                })
                .otherwise(t -> {
                    LOG.warn("failed to resolve asset [{}] in tenant [{}]: {}",
                            assetId, tenantId, t.getMessage());
                    return Optional.empty();
                });
    }

    /**
     * Represents a resolved mapping from an EDC asset to a Hono device.
     *
     * @param tenantId The Hono tenant ID.
     * @param deviceId The Hono device ID.
     */
    public record DeviceMapping(String tenantId, String deviceId) {

        /**
         * Creates a new device mapping.
         *
         * @param tenantId The tenant ID.
         * @param deviceId The device ID.
         * @throws NullPointerException if any parameter is {@code null}.
         */
        public DeviceMapping {
            Objects.requireNonNull(tenantId);
            Objects.requireNonNull(deviceId);
        }
    }
}
