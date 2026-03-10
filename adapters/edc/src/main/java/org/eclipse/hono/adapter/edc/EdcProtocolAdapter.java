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

import java.util.Optional;

import org.eclipse.hono.adapter.AbstractProtocolAdapterBase;
import org.eclipse.hono.adapter.edc.client.EdcDataPlaneClient;
import org.eclipse.hono.adapter.edc.client.EdcManagementClient;
import org.eclipse.hono.adapter.edc.mapping.DeviceAssetMapper;
import org.eclipse.hono.adapter.edc.polling.TelemetryPollingService;
import org.eclipse.hono.util.Constants;

import io.vertx.core.Promise;

/**
 * A Hono protocol adapter that periodically fetches telemetry data from an
 * EDC Consumer connector and forwards it into Hono's messaging infrastructure.
 */
public final class EdcProtocolAdapter extends AbstractProtocolAdapterBase<EdcAdapterProperties> {

    /**
     * The type name for this adapter.
     */
    public static final String TYPE_NAME = "hono-edc";

    private EdcAdapterMetrics metrics;
    private TelemetryPollingService pollingService;

    /**
     * {@inheritDoc}
     *
     * @return {@value #TYPE_NAME}
     */
    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The EDC adapter does not expose a secure port.
     *
     * @return {@value Constants#PORT_UNCONFIGURED}
     */
    @Override
    public int getPortDefaultValue() {
        return Constants.PORT_UNCONFIGURED;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The EDC adapter does not expose an insecure port.
     *
     * @return {@value Constants#PORT_UNCONFIGURED}
     */
    @Override
    public int getInsecurePortDefaultValue() {
        return Constants.PORT_UNCONFIGURED;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The EDC adapter does not expose a secure port.
     */
    @Override
    protected int getActualPort() {
        return Constants.PORT_UNCONFIGURED;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The EDC adapter does not expose an insecure port.
     */
    @Override
    protected int getActualInsecurePort() {
        return Constants.PORT_UNCONFIGURED;
    }

    /**
     * Sets the metrics for this adapter.
     *
     * @param metrics The metrics.
     */
    public void setMetrics(final EdcAdapterMetrics metrics) {
        Optional.ofNullable(metrics)
                .ifPresent(m -> log.info("reporting metrics using [{}]", m.getClass().getName()));
        this.metrics = metrics;
    }

    /**
     * Gets the metrics for this adapter.
     *
     * @return The metrics, or {@code null} if not set.
     */
    public EdcAdapterMetrics getMetrics() {
        return metrics;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Creates the EDC Management API client, Data Plane client, and polling service,
     * then starts periodic telemetry fetching.
     */
    @Override
    protected void doStart(final Promise<Void> startPromise) {
        log.info("EDC protocol adapter starting");

        final var config = getConfig();
        if (config.getManagementApiUrl() == null || config.getManagementApiKey() == null) {
            log.error("EDC management API URL and API key must be configured");
            startPromise.fail("EDC management API configuration missing");
            return;
        }

        final var managementClient = new EdcManagementClient(
                vertx,
                config.getManagementApiUrl(),
                config.getManagementApiKey(),
                config.getMaxRetries(),
                config.getRetryBackoffBase(),
                config.getNegotiationPollInterval(),
                config.getNegotiationTimeout(),
                config.getTransferPollInterval(),
                config.getTransferTimeout());

        final var dataPlaneClient = new EdcDataPlaneClient(vertx, config.getDataPlaneRequestTimeout());
        final var deviceAssetMapper = new DeviceAssetMapper(getRegistrationClient());

        pollingService = new TelemetryPollingService(
                vertx,
                managementClient,
                dataPlaneClient,
                this::getTelemetrySender,
                getTenantClient(),
                deviceAssetMapper,
                config,
                metrics != null ? metrics : EdcAdapterMetrics.NOOP);

        if (config.getAssetIdFilter() != null) {
            log.info("Asset filter configured: assetId={}", config.getAssetIdFilter());
        } else {
            log.info("No asset filter configured, processing all catalog assets");
        }
        pollingService.start();
        log.info("EDC protocol adapter started [pollingInterval={}]", config.getPollingInterval());
        startPromise.complete();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Stops the polling service and cancels the periodic timer.
     */
    @Override
    protected void doStop(final Promise<Void> stopPromise) {
        log.info("EDC protocol adapter stopping");
        if (pollingService != null) {
            pollingService.stop();
        }
        stopPromise.complete();
    }
}
