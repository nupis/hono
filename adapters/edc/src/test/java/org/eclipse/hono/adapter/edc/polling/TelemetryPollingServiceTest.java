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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.eclipse.hono.adapter.edc.EdcAdapterMetrics;
import org.eclipse.hono.adapter.edc.EdcAdapterProperties;
import org.eclipse.hono.adapter.edc.client.EdcDataPlaneClient;
import org.eclipse.hono.adapter.edc.client.EdcDataPlaneClient.TelemetryPullResult;
import org.eclipse.hono.adapter.edc.client.EdcManagementClient;
import org.eclipse.hono.adapter.edc.client.model.CatalogAsset;
import org.eclipse.hono.adapter.edc.client.model.EndpointDataReference;
import org.eclipse.hono.adapter.edc.mapping.DeviceAssetMapper;
import org.eclipse.hono.adapter.edc.mapping.DeviceAssetMapper.DeviceMapping;
import org.eclipse.hono.client.registry.TenantClient;
import org.eclipse.hono.client.telemetry.TelemetrySender;
import org.eclipse.hono.util.QoS;
import org.eclipse.hono.util.RegistrationAssertion;
import org.eclipse.hono.util.TenantObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

/**
 * Tests for {@link TelemetryPollingService}.
 */
@ExtendWith(VertxExtension.class)
class TelemetryPollingServiceTest {

    private static final String TENANT_ID = "DEFAULT_TENANT";
    private static final String DEVICE_ID = "sensor-001";
    private static final String PROVIDER_DSP_URL = "https://provider.example.com/dsp";
    private static final String OFFER_ID = "offer-123";
    private static final String NEGOTIATION_ID = "neg-001";
    private static final String AGREEMENT_ID = "agreement-001";
    private static final String TRANSFER_ID = "transfer-001";
    private static final byte[] PAYLOAD = "telemetry-data".getBytes();
    private static final String CONTENT_TYPE = "application/json";

    private EdcManagementClient managementClient;
    private EdcDataPlaneClient dataPlaneClient;
    private TelemetrySender telemetrySender;
    private TenantClient tenantClient;
    private DeviceAssetMapper deviceAssetMapper;
    private EdcAdapterProperties properties;
    private EdcAdapterMetrics metrics;
    private TelemetryPollingService pollingService;

    @BeforeEach
    void setUp(final Vertx vertx) {
        managementClient = mock(EdcManagementClient.class);
        dataPlaneClient = mock(EdcDataPlaneClient.class);
        telemetrySender = mock(TelemetrySender.class);
        tenantClient = mock(TenantClient.class);
        deviceAssetMapper = mock(DeviceAssetMapper.class);
        properties = new EdcAdapterProperties();
        properties.setProviderDspUrl(PROVIDER_DSP_URL);
        metrics = EdcAdapterMetrics.NOOP;

        pollingService = new TelemetryPollingService(
                vertx,
                managementClient,
                dataPlaneClient,
                tenant -> telemetrySender,
                tenantClient,
                deviceAssetMapper,
                properties,
                metrics);
    }

    @Test
    void testExecuteCycleQueriesCatalogAndProcessesEachAsset(final Vertx vertx, final VertxTestContext ctx) {
        final var asset = new CatalogAsset(DEVICE_ID, OFFER_ID, PROVIDER_DSP_URL);
        setupSuccessfulFlow(asset);

        pollingService.executeCycle()
                .onComplete(ctx.succeeding(v -> {
                    ctx.verify(() -> {
                        verify(managementClient).queryCatalog(PROVIDER_DSP_URL);
                        verify(managementClient).initiateNegotiation(PROVIDER_DSP_URL, OFFER_ID, DEVICE_ID);
                    });
                    ctx.completeNow();
                }));
    }

    @Test
    void testForwardsToTelemetrySenderWithCorrectMetadata(final Vertx vertx, final VertxTestContext ctx) {
        final var asset = new CatalogAsset(DEVICE_ID, OFFER_ID, PROVIDER_DSP_URL);
        setupSuccessfulFlow(asset);

        pollingService.executeCycle()
                .onComplete(ctx.succeeding(v -> {
                    ctx.verify(() -> verify(telemetrySender).sendTelemetry(
                            any(TenantObject.class),
                            any(RegistrationAssertion.class),
                            eq(QoS.AT_LEAST_ONCE),
                            eq(CONTENT_TYPE),
                            any(Buffer.class),
                            any(),
                            any()));
                    ctx.completeNow();
                }));
    }

    @Test
    void testSkipsDeviceWhenNegotiationFails(final Vertx vertx, final VertxTestContext ctx) {
        final var asset1 = new CatalogAsset("failing-device", OFFER_ID, PROVIDER_DSP_URL);
        final var asset2 = new CatalogAsset(DEVICE_ID, "offer-2", PROVIDER_DSP_URL);

        when(managementClient.queryCatalog(PROVIDER_DSP_URL))
                .thenReturn(Future.succeededFuture(List.of(asset1, asset2)));

        // First device: mapper resolves but negotiation fails
        when(deviceAssetMapper.resolveDevice(eq(TENANT_ID), eq("failing-device"), any()))
                .thenReturn(Future.succeededFuture(Optional.of(new DeviceMapping(TENANT_ID, "failing-device"))));
        when(managementClient.initiateNegotiation(PROVIDER_DSP_URL, OFFER_ID, "failing-device"))
                .thenReturn(Future.failedFuture(new RuntimeException("negotiation failed")));

        // Second device: full success
        setupDeviceFlow(asset2);

        // Need tenant for both
        when(tenantClient.get(eq(TENANT_ID), any()))
                .thenReturn(Future.succeededFuture(TenantObject.from(TENANT_ID)));

        pollingService.executeCycle()
                .onComplete(ctx.succeeding(v -> {
                    ctx.verify(() -> {
                        // Should still process second device
                        verify(managementClient).initiateNegotiation(PROVIDER_DSP_URL, "offer-2", DEVICE_ID);
                        verify(telemetrySender).sendTelemetry(
                                any(), any(), any(), eq(CONTENT_TYPE), any(), any(), any());
                    });
                    ctx.completeNow();
                }));
    }

    @Test
    void testSkipsAssetWithNoMatchingDevice(final Vertx vertx, final VertxTestContext ctx) {
        final var asset = new CatalogAsset("unknown-device", OFFER_ID, PROVIDER_DSP_URL);

        when(managementClient.queryCatalog(PROVIDER_DSP_URL))
                .thenReturn(Future.succeededFuture(List.of(asset)));
        when(deviceAssetMapper.resolveDevice(eq(TENANT_ID), eq("unknown-device"), any()))
                .thenReturn(Future.succeededFuture(Optional.empty()));
        when(tenantClient.get(eq(TENANT_ID), any()))
                .thenReturn(Future.succeededFuture(TenantObject.from(TENANT_ID)));

        pollingService.executeCycle()
                .onComplete(ctx.succeeding(v -> {
                    ctx.verify(() -> {
                        verify(managementClient, never()).initiateNegotiation(any(), any(), any());
                        verify(telemetrySender, never()).sendTelemetry(
                                any(), any(), any(), any(), any(), any(), any());
                    });
                    ctx.completeNow();
                }));
    }

    @Test
    void testHandlesEmptyCatalogGracefully(final Vertx vertx, final VertxTestContext ctx) {
        when(managementClient.queryCatalog(PROVIDER_DSP_URL))
                .thenReturn(Future.succeededFuture(List.of()));

        pollingService.executeCycle()
                .onComplete(ctx.succeeding(v -> {
                    ctx.verify(() -> {
                        verify(managementClient, never()).initiateNegotiation(any(), any(), any());
                        verify(telemetrySender, never()).sendTelemetry(
                                any(), any(), any(), any(), any(), any(), any());
                    });
                    ctx.completeNow();
                }));
    }

    private void setupSuccessfulFlow(final CatalogAsset asset) {
        when(managementClient.queryCatalog(PROVIDER_DSP_URL))
                .thenReturn(Future.succeededFuture(List.of(asset)));
        setupDeviceFlow(asset);
        when(tenantClient.get(eq(TENANT_ID), any()))
                .thenReturn(Future.succeededFuture(TenantObject.from(TENANT_ID)));
    }

    private void setupDeviceFlow(final CatalogAsset asset) {
        when(deviceAssetMapper.resolveDevice(eq(TENANT_ID), eq(asset.assetId()), any()))
                .thenReturn(Future.succeededFuture(Optional.of(new DeviceMapping(TENANT_ID, asset.assetId()))));
        when(managementClient.initiateNegotiation(PROVIDER_DSP_URL, asset.offerId(), asset.assetId()))
                .thenReturn(Future.succeededFuture(NEGOTIATION_ID));
        when(managementClient.awaitNegotiationFinalized(NEGOTIATION_ID))
                .thenReturn(Future.succeededFuture(AGREEMENT_ID));
        when(managementClient.initiateTransfer(PROVIDER_DSP_URL, AGREEMENT_ID))
                .thenReturn(Future.succeededFuture(TRANSFER_ID));
        when(managementClient.awaitTransferStarted(TRANSFER_ID))
                .thenReturn(Future.succeededFuture());
        when(managementClient.getEndpointDataReference(TRANSFER_ID))
                .thenReturn(Future.succeededFuture(new EndpointDataReference(
                        "DataAddress", "HttpData", "https://dataplane.example.com/data", "Bearer token123")));
        when(dataPlaneClient.pullTelemetry(any(EndpointDataReference.class)))
                .thenReturn(Future.succeededFuture(new TelemetryPullResult(PAYLOAD, CONTENT_TYPE)));
        when(telemetrySender.sendTelemetry(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Future.succeededFuture());
    }
}
