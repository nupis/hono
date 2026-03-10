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
package org.eclipse.hono.adapter.edc.polling;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.eclipse.hono.adapter.edc.EdcAdapterMetrics;
import org.eclipse.hono.adapter.edc.EdcAdapterProperties;
import org.eclipse.hono.adapter.edc.client.EdcDataPlaneClient;
import org.eclipse.hono.adapter.edc.client.EdcManagementClient;
import org.eclipse.hono.adapter.edc.client.model.CatalogAsset;
import org.eclipse.hono.adapter.edc.mapping.DeviceAssetMapper;
import org.eclipse.hono.client.registry.TenantClient;
import org.eclipse.hono.client.telemetry.TelemetrySender;
import org.eclipse.hono.util.QoS;
import org.eclipse.hono.util.RegistrationAssertion;
import org.eclipse.hono.util.TenantObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

/**
 * Orchestrates the periodic polling of an EDC Consumer connector for telemetry data.
 * <p>
 * Each polling cycle: queries the EDC catalog, iterates over discovered assets,
 * resolves each to a Hono device, negotiates a contract, transfers data via
 * HttpData-PULL, and forwards the payload to Hono's messaging infrastructure.
 */
public class TelemetryPollingService {

    private static final Logger LOG = LoggerFactory.getLogger(TelemetryPollingService.class);

    private final Vertx vertx;
    private final EdcManagementClient managementClient;
    private final EdcDataPlaneClient dataPlaneClient;
    private final Function<TenantObject, TelemetrySender> telemetrySenderProvider;
    private final TenantClient tenantClient;
    private final DeviceAssetMapper deviceAssetMapper;
    private final EdcAdapterProperties properties;
    private final EdcAdapterMetrics metrics;
    private final AtomicBoolean cycleInProgress = new AtomicBoolean(false);
    private Long timerId;

    /**
     * Creates a new TelemetryPollingService.
     *
     * @param vertx The Vert.x instance.
     * @param managementClient The EDC Management API client.
     * @param dataPlaneClient The EDC Data Plane client.
     * @param telemetrySenderProvider Function to obtain a TelemetrySender for a given tenant.
     * @param tenantClient The Hono tenant client.
     * @param deviceAssetMapper The device-to-asset mapper.
     * @param properties The adapter configuration.
     * @param metrics The adapter metrics.
     */
    public TelemetryPollingService(
            final Vertx vertx,
            final EdcManagementClient managementClient,
            final EdcDataPlaneClient dataPlaneClient,
            final Function<TenantObject, TelemetrySender> telemetrySenderProvider,
            final TenantClient tenantClient,
            final DeviceAssetMapper deviceAssetMapper,
            final EdcAdapterProperties properties,
            final EdcAdapterMetrics metrics) {

        this.vertx = Objects.requireNonNull(vertx);
        this.managementClient = Objects.requireNonNull(managementClient);
        this.dataPlaneClient = Objects.requireNonNull(dataPlaneClient);
        this.telemetrySenderProvider = Objects.requireNonNull(telemetrySenderProvider);
        this.tenantClient = Objects.requireNonNull(tenantClient);
        this.deviceAssetMapper = Objects.requireNonNull(deviceAssetMapper);
        this.properties = Objects.requireNonNull(properties);
        this.metrics = Objects.requireNonNull(metrics);
    }

    /**
     * Starts the periodic polling timer.
     */
    public void start() {
        final long intervalMs = properties.getPollingInterval().toMillis();
        LOG.info("starting EDC polling service [interval={}ms]", intervalMs);
        timerId = vertx.setPeriodic(intervalMs, id -> executeCycle());
    }

    /**
     * Stops the periodic polling timer.
     */
    public void stop() {
        if (timerId != null) {
            LOG.info("stopping EDC polling service");
            vertx.cancelTimer(timerId);
            timerId = null;
        }
    }

    /**
     * Executes a single polling cycle.
     *
     * @return A future that completes when the cycle is done.
     */
    public Future<Void> executeCycle() {
        if (!cycleInProgress.compareAndSet(false, true)) {
            LOG.warn("polling cycle already in progress, skipping");
            return Future.succeededFuture();
        }

        final String cycleCorrelationId = UUID.randomUUID().toString();
        final String assetIdFilter = properties.getAssetIdFilter();
        final String filterInfo = assetIdFilter != null ? assetIdFilter : "all";
        LOG.info("starting polling cycle [correlationId={}, assetFilter={}]",
                cycleCorrelationId, filterInfo);
        final Instant cycleStart = Instant.now();

        return managementClient.queryCatalog(properties.getProviderDspUrl(), properties.getProviderBpn())
                .compose(assets -> {
                    final var filtered = filterAssets(assets, assetIdFilter, cycleCorrelationId);
                    LOG.info("discovered {} assets in catalog, processing {} after filtering [correlationId={}]",
                            assets.size(), filtered.size(), cycleCorrelationId);
                    return processAssetsSequentially(filtered, cycleCorrelationId);
                })
                .onSuccess(v -> {
                    final Duration elapsed = Duration.between(cycleStart, Instant.now());
                    LOG.info("polling cycle completed [correlationId={}, duration={}ms]",
                            cycleCorrelationId, elapsed.toMillis());
                    metrics.recordCycleCompleted();
                })
                .onFailure(t -> LOG.error("polling cycle failed [correlationId={}]",
                        cycleCorrelationId, t))
                .eventually(() -> {
                    cycleInProgress.set(false);
                    return Future.succeededFuture();
                });
    }

    private List<CatalogAsset> filterAssets(
            final List<CatalogAsset> assets,
            final String assetIdFilter,
            final String cycleCorrelationId) {

        if (assetIdFilter == null) {
            return assets;
        }

        final var filtered = assets.stream()
                .filter(asset -> asset.assetId().equals(assetIdFilter))
                .toList();

        if (filtered.isEmpty()) {
            LOG.warn("filtered asset {} not found in catalog ({} assets discovered) [correlationId={}]",
                    assetIdFilter, assets.size(), cycleCorrelationId);
        }

        return filtered;
    }

    private Future<Void> processAssetsSequentially(
            final java.util.List<CatalogAsset> assets,
            final String cycleCorrelationId) {

        Future<Void> chain = Future.succeededFuture();
        for (final CatalogAsset asset : assets) {
            chain = chain.compose(v -> processAsset(asset, cycleCorrelationId)
                    .recover(t -> {
                        LOG.error("failed to process asset [assetId={}, correlationId={}]",
                                asset.assetId(), cycleCorrelationId, t);
                        return Future.succeededFuture();
                    }));
        }
        return chain;
    }

    private Future<Void> processAsset(final CatalogAsset asset, final String cycleCorrelationId) {
        final String correlationId = cycleCorrelationId + ":" + asset.assetId();
        LOG.debug("processing asset [assetId={}, correlationId={}]", asset.assetId(), correlationId);
        final Instant fetchStart = Instant.now();

        // Resolve the device in each known tenant
        // For now, we use a single tenant from configuration context
        // The polling service iterates tenants externally
        return resolveDeviceAcrossTenants(asset.assetId(), correlationId)
                .compose(mappingOpt -> {
                    if (mappingOpt.isEmpty()) {
                        LOG.warn("no matching device found for asset [assetId={}, correlationId={}]",
                                asset.assetId(), correlationId);
                        return Future.succeededFuture();
                    }
                    final var mapping = mappingOpt.get();
                    return negotiateAndTransfer(asset, mapping, correlationId)
                            .onSuccess(v -> {
                                metrics.recordFetchLatency(Duration.between(fetchStart, Instant.now()));
                                metrics.updateLastSuccessfulFetch();
                            });
                });
    }

    private Future<java.util.Optional<DeviceAssetMapper.DeviceMapping>> resolveDeviceAcrossTenants(
            final String assetId,
            final String correlationId) {

        // The DeviceAssetMapper resolves in a known tenant context
        // For the MVP, we iterate known tenants via TenantClient
        // Since device IDs are globally unique, we try with a placeholder tenant
        // In production, the adapter would iterate configured tenants
        return tenantClient.get("DEFAULT_TENANT", null)
                .compose(tenant -> deviceAssetMapper.resolveDevice(tenant.getTenantId(), assetId, null))
                .recover(t -> {
                    LOG.debug("device resolution failed [assetId={}, correlationId={}]: {}",
                            assetId, correlationId, t.getMessage());
                    return Future.succeededFuture(java.util.Optional.empty());
                });
    }

    private Future<Void> negotiateAndTransfer(
            final CatalogAsset asset,
            final DeviceAssetMapper.DeviceMapping mapping,
            final String correlationId) {

        return managementClient.initiateNegotiation(
                        asset.providerDspUrl(), properties.getProviderBpn(), asset.offerId(), asset.assetId())
                .compose(negotiationId -> {
                    LOG.debug("negotiation initiated [negotiationId={}, correlationId={}]",
                            negotiationId, correlationId);
                    return managementClient.awaitNegotiationFinalized(negotiationId);
                })
                .onSuccess(v -> metrics.recordNegotiationSuccess())
                .onFailure(t -> metrics.recordNegotiationFailure())
                .compose(agreementId -> {
                    LOG.debug("negotiation finalized [agreementId={}, correlationId={}]",
                            agreementId, correlationId);
                    return managementClient.initiateTransfer(
                            asset.providerDspUrl(), properties.getProviderBpn(), agreementId);
                })
                .compose(transferId -> {
                    LOG.debug("transfer initiated [transferId={}, correlationId={}]",
                            transferId, correlationId);
                    return managementClient.awaitTransferStarted(transferId)
                            .map(v -> transferId);
                })
                .onSuccess(v -> metrics.recordTransferSuccess())
                .onFailure(t -> metrics.recordTransferFailure())
                .compose(transferId -> managementClient.getEndpointDataReference(transferId))
                .compose(edr -> dataPlaneClient.pullTelemetry(edr))
                .compose(pullResult -> forwardTelemetry(mapping, pullResult, correlationId));
    }

    private Future<Void> forwardTelemetry(
            final DeviceAssetMapper.DeviceMapping mapping,
            final EdcDataPlaneClient.TelemetryPullResult pullResult,
            final String correlationId) {

        // Validate payload (FR-012)
        if (pullResult.payload() == null || pullResult.payload().length == 0) {
            LOG.warn("empty or null telemetry payload, skipping [device={}, correlationId={}]",
                    mapping.deviceId(), correlationId);
            return Future.succeededFuture();
        }

        return tenantClient.get(mapping.tenantId(), null)
                .compose(tenantObject -> {
                    // Check tenant is enabled (FR-007)
                    if (!tenantObject.isEnabled()) {
                        LOG.warn("tenant disabled, skipping telemetry [tenant={}, correlationId={}]",
                                mapping.tenantId(), correlationId);
                        return Future.<Void>succeededFuture();
                    }

                    final var registrationAssertion = new RegistrationAssertion(mapping.deviceId());

                    final Map<String, Object> props = Map.of(
                            "correlation-id", correlationId,
                            "source", "edc-adapter");

                    LOG.debug("forwarding telemetry [tenant={}, device={}, contentType={}, correlationId={}]",
                            mapping.tenantId(), mapping.deviceId(), pullResult.contentType(), correlationId);

                    final var sender = telemetrySenderProvider.apply(tenantObject);
                    return sender.sendTelemetry(
                            tenantObject,
                            registrationAssertion,
                            QoS.AT_LEAST_ONCE,
                            pullResult.contentType(),
                            Buffer.buffer(pullResult.payload()),
                            props,
                            null)
                            .onSuccess(v -> {
                                metrics.recordTelemetryForwarded();
                                LOG.info("telemetry forwarded [tenant={}, device={}, correlationId={}]",
                                        mapping.tenantId(), mapping.deviceId(), correlationId);
                            });
                });
    }
}
