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

import java.net.HttpURLConnection;

import org.eclipse.hono.client.ClientErrorException;
import org.eclipse.hono.client.ServerErrorException;
import org.eclipse.hono.client.registry.DeviceRegistrationClient;
import org.eclipse.hono.util.RegistrationAssertion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

/**
 * Tests for {@link DeviceAssetMapper}.
 */
@ExtendWith(VertxExtension.class)
class DeviceAssetMapperTest {

    private static final String TENANT_ID = "test-tenant";
    private static final String DEVICE_ID = "sensor-001";

    private DeviceRegistrationClient registrationClient;
    private DeviceAssetMapper mapper;

    @BeforeEach
    void setUp() {
        registrationClient = Mockito.mock(DeviceRegistrationClient.class);
        mapper = new DeviceAssetMapper(registrationClient);
    }

    @Test
    void testResolveDeviceWithKnownDeviceReturnsTenantAndDeviceId(final Vertx vertx, final VertxTestContext ctx) {
        final RegistrationAssertion assertion = new RegistrationAssertion(DEVICE_ID);
        Mockito.when(registrationClient.assertRegistration(
                        ArgumentMatchers.eq(TENANT_ID), ArgumentMatchers.eq(DEVICE_ID),
                        ArgumentMatchers.isNull(), ArgumentMatchers.any()))
                .thenReturn(Future.succeededFuture(assertion));

        mapper.resolveDevice(TENANT_ID, DEVICE_ID, null)
                .onComplete(ctx.succeeding(result -> {
                    ctx.verify(() -> {
                        Assertions.assertTrue(result.isPresent());
                        Assertions.assertEquals(TENANT_ID, result.get().tenantId());
                        Assertions.assertEquals(DEVICE_ID, result.get().deviceId());
                    });
                    ctx.completeNow();
                }));
    }

    @Test
    void testResolveDeviceWithUnknownAssetIdReturnsEmpty(final Vertx vertx, final VertxTestContext ctx) {
        Mockito.when(registrationClient.assertRegistration(
                        ArgumentMatchers.eq(TENANT_ID), ArgumentMatchers.eq("unknown-device"),
                        ArgumentMatchers.isNull(), ArgumentMatchers.any()))
                .thenReturn(Future.failedFuture(
                        new ClientErrorException(HttpURLConnection.HTTP_NOT_FOUND)));

        mapper.resolveDevice(TENANT_ID, "unknown-device", null)
                .onComplete(ctx.succeeding(result -> {
                    ctx.verify(() -> Assertions.assertTrue(result.isEmpty()));
                    ctx.completeNow();
                }));
    }

    @Test
    void testResolveDeviceWithRegistrationClientFailureReturnsEmpty(final Vertx vertx, final VertxTestContext ctx) {
        Mockito.when(registrationClient.assertRegistration(
                        ArgumentMatchers.eq(TENANT_ID), ArgumentMatchers.eq(DEVICE_ID),
                        ArgumentMatchers.isNull(), ArgumentMatchers.any()))
                .thenReturn(Future.failedFuture(
                        new ServerErrorException(HttpURLConnection.HTTP_UNAVAILABLE)));

        mapper.resolveDevice(TENANT_ID, DEVICE_ID, null)
                .onComplete(ctx.succeeding(result -> {
                    ctx.verify(() -> Assertions.assertTrue(result.isEmpty()));
                    ctx.completeNow();
                }));
    }

    @Test
    void testResolveDeviceCallsAssertRegistration(final Vertx vertx, final VertxTestContext ctx) {
        final RegistrationAssertion assertion = new RegistrationAssertion(DEVICE_ID);
        Mockito.when(registrationClient.assertRegistration(
                        ArgumentMatchers.eq(TENANT_ID), ArgumentMatchers.eq(DEVICE_ID),
                        ArgumentMatchers.isNull(), ArgumentMatchers.any()))
                .thenReturn(Future.succeededFuture(assertion));

        mapper.resolveDevice(TENANT_ID, DEVICE_ID, null)
                .onComplete(ctx.succeeding(result -> {
                    ctx.verify(() -> Mockito.verify(registrationClient)
                            .assertRegistration(
                                    ArgumentMatchers.eq(TENANT_ID), ArgumentMatchers.eq(DEVICE_ID),
                                    ArgumentMatchers.isNull(), ArgumentMatchers.any()));
                    ctx.completeNow();
                }));
    }

    @Test
    void testResolveDeviceWithNullAssetIdFails() {
        Assertions.assertThrows(NullPointerException.class,
                () -> mapper.resolveDevice(TENANT_ID, null, null));
    }
}
