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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.eclipse.hono.adapter.test.ProtocolAdapterTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

/**
 * Tests for {@link EdcProtocolAdapter}.
 */
class EdcProtocolAdapterTest extends ProtocolAdapterTestSupport<EdcAdapterProperties, EdcProtocolAdapter> {

    private Vertx vertx;

    @Override
    protected EdcAdapterProperties givenDefaultConfigurationProperties() {
        return new EdcAdapterProperties();
    }

    @BeforeEach
    void setUp() {
        vertx = mock(Vertx.class);
        when(vertx.setPeriodic(anyLong(), anyLong(), org.mockito.ArgumentMatchers.<Handler<Long>>any()))
                .thenReturn(42L);

        createClients();
        properties = givenDefaultConfigurationProperties();
        adapter = new EdcProtocolAdapter();
        adapter.setConfig(properties);
        adapter.setMetrics(EdcAdapterMetrics.NOOP);
        setServiceClients(adapter);
    }

    @Test
    void getTypeNameReturnsHonoEdc() {
        assertEquals("hono-edc", adapter.getTypeName());
    }

    @Test
    void getTypeNameMatchesConstant() {
        assertEquals(EdcProtocolAdapter.TYPE_NAME, adapter.getTypeName());
    }

    @Test
    void doStartFailsWhenManagementApiNotConfigured() {
        final Promise<Void> startPromise = Promise.promise();

        adapter.doStart(startPromise);

        assertEquals(true, startPromise.future().failed());
    }

    @Test
    void doStartFailsWhenProviderBpnNotConfigured() {
        properties.setManagementApiUrl("https://edc-consumer:8181");
        properties.setManagementApiKey("test-key");

        final Promise<Void> startPromise = Promise.promise();
        adapter.doStart(startPromise);

        assertEquals(true, startPromise.future().failed());
    }

    @Test
    void doStartSucceedsWhenAllRequiredPropertiesConfigured() {
        properties.setManagementApiUrl("https://edc-consumer:8181");
        properties.setManagementApiKey("test-key");
        properties.setProviderDspUrl("https://edc-provider:8282/api/dsp");
        properties.setProviderBpn("BPNL00000003CRHK");

        // Mock setPeriodic for the 2-arg variant too
        when(vertx.setPeriodic(anyLong(), org.mockito.ArgumentMatchers.<Handler<Long>>any()))
                .thenReturn(42L);

        adapter.init(vertx, null);
        final Promise<Void> startPromise = Promise.promise();
        adapter.doStart(startPromise);

        assertEquals(true, startPromise.future().succeeded());
    }

    @Test
    void doStopCompletesSuccessfully() {
        final Promise<Void> stopPromise = Promise.promise();

        adapter.doStop(stopPromise);

        assertEquals(true, stopPromise.future().succeeded());
    }

    @Test
    void pollingIntervalReadFromProperties() {
        final Duration expectedInterval = Duration.ofSeconds(120);
        properties.setPollingInterval(expectedInterval);

        assertEquals(expectedInterval, properties.getPollingInterval());
    }
}
