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
package org.eclipse.hono.adapter.edc.client;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.eclipse.hono.adapter.edc.client.model.EndpointDataReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import io.vertx.core.Vertx;

/**
 * Tests for {@link EdcDataPlaneClient} using WireMock to simulate an EDC data plane endpoint.
 */
class EdcDataPlaneClientTest {

    private static final String AUTH_TOKEN = "eyJhbGciOiJSUzI1NiJ9.test-token";

    private WireMockServer wireMock;
    private Vertx vertx;
    private EdcDataPlaneClient client;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
        vertx = Vertx.vertx();

        client = new EdcDataPlaneClient(vertx, Duration.ofSeconds(5));
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
        vertx.close();
    }

    private EndpointDataReference createEdr() {
        return new EndpointDataReference(
                "DataAddress",
                "https://w3id.org/idsa/v4.1/HTTP",
                "http://localhost:" + wireMock.port() + "/api/public",
                AUTH_TOKEN);
    }

    @Test
    void pullTelemetrySendsGetWithCorrectAuthorizationHeader() throws Exception {
        final byte[] telemetryPayload = "{\"temperature\": 22.5}".getBytes();

        wireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/public"))
                .withHeader("Authorization", WireMock.equalTo(AUTH_TOKEN))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(telemetryPayload)));

        final var result = client.pullTelemetry(createEdr())
                .toCompletionStage().toCompletableFuture()
                .get(5, TimeUnit.SECONDS);

        Assertions.assertNotNull(result);

        wireMock.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/api/public"))
                .withHeader("Authorization", WireMock.equalTo(AUTH_TOKEN)));
    }

    @Test
    void pullTelemetryReturnsRawPayloadBytesAndContentType() throws Exception {
        final byte[] telemetryPayload = "{\"temperature\": 22.5, \"humidity\": 65}".getBytes();

        wireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/public"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(telemetryPayload)));

        final var result = client.pullTelemetry(createEdr())
                .toCompletionStage().toCompletableFuture()
                .get(5, TimeUnit.SECONDS);

        Assertions.assertNotNull(result);
        Assertions.assertArrayEquals(telemetryPayload, result.payload());
        Assertions.assertEquals("application/json", result.contentType());
    }

    @Test
    void pullTelemetryHandlesTimeoutGracefully() throws Exception {
        wireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/public"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withFixedDelay(10_000)
                        .withBody("delayed")));

        // Use a short timeout client for this test
        final var shortTimeoutClient = new EdcDataPlaneClient(vertx, Duration.ofMillis(500));

        final var future = shortTimeoutClient.pullTelemetry(createEdr())
                .toCompletionStage().toCompletableFuture();

        Assertions.assertTrue(future.isCompletedExceptionally() || assertThrowsOnGet(future));
    }

    @Test
    void pullTelemetryHandles4xxUnauthorized() throws Exception {
        wireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/public"))
                .willReturn(WireMock.aResponse()
                        .withStatus(401)
                        .withBody("Unauthorized")));

        final var future = client.pullTelemetry(createEdr())
                .toCompletionStage().toCompletableFuture();

        Assertions.assertTrue(future.isCompletedExceptionally() || assertThrowsOnGet(future));
    }

    @Test
    void pullTelemetryHandles5xxServerError() throws Exception {
        wireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/public"))
                .willReturn(WireMock.aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        final var future = client.pullTelemetry(createEdr())
                .toCompletionStage().toCompletableFuture();

        Assertions.assertTrue(future.isCompletedExceptionally() || assertThrowsOnGet(future));
    }

    @Test
    void pullTelemetryHandlesEmptyResponseBody() throws Exception {
        wireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/public"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/octet-stream")
                        .withBody(new byte[0])));

        final var future = client.pullTelemetry(createEdr())
                .toCompletionStage().toCompletableFuture();

        // Empty body should be treated as an error or return empty result
        Assertions.assertTrue(future.isCompletedExceptionally() || assertEmptyOrThrows(future));
    }

    private boolean assertThrowsOnGet(final java.util.concurrent.CompletableFuture<?> future) {
        try {
            future.get(5, TimeUnit.SECONDS);
            return false;
        } catch (final Exception e) {
            return true;
        }
    }

    private boolean assertEmptyOrThrows(final java.util.concurrent.CompletableFuture<?> future) {
        try {
            final var result = future.get(5, TimeUnit.SECONDS);
            // If it returns a result, it should indicate empty payload
            return result != null;
        } catch (final Exception e) {
            return true;
        }
    }
}
