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
import java.util.Objects;

import org.eclipse.hono.adapter.edc.client.model.EndpointDataReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * Client for pulling telemetry data from an EDC data plane endpoint.
 * <p>
 * Uses the Endpoint Data Reference (EDR) obtained after a successful transfer
 * to pull raw telemetry data from the provider's data plane public API.
 */
public class EdcDataPlaneClient {

    private static final Logger LOG = LoggerFactory.getLogger(EdcDataPlaneClient.class);

    private final WebClient webClient;
    private final long requestTimeoutMs;

    /**
     * A result containing the raw telemetry payload and its content type.
     *
     * @param payload The raw telemetry data bytes.
     * @param contentType The MIME type of the telemetry payload.
     */
    public record TelemetryPullResult(byte[] payload, String contentType) {
    }

    /**
     * Creates a new EdcDataPlaneClient.
     *
     * @param vertx The Vert.x instance for async HTTP operations.
     * @param requestTimeout The timeout for data plane HTTP requests.
     */
    public EdcDataPlaneClient(final Vertx vertx, final Duration requestTimeout) {
        Objects.requireNonNull(vertx, "vertx must not be null");
        Objects.requireNonNull(requestTimeout, "requestTimeout must not be null");

        this.webClient = WebClient.create(vertx, new WebClientOptions());
        this.requestTimeoutMs = requestTimeout.toMillis();
    }

    /**
     * Pulls telemetry data from the EDC data plane using the given EDR.
     *
     * @param edr The Endpoint Data Reference with data plane URL and authorization token.
     * @return A future containing the telemetry pull result with payload and content type.
     */
    public Future<TelemetryPullResult> pullTelemetry(final EndpointDataReference edr) {
        Objects.requireNonNull(edr, "edr must not be null");
        Objects.requireNonNull(edr.endpoint(), "edr.endpoint must not be null");

        LOG.debug("pulling telemetry from data plane [endpoint={}]", edr.endpoint());

        return webClient.requestAbs(HttpMethod.GET, edr.endpoint())
                .putHeader("Authorization", edr.authorization())
                .timeout(requestTimeoutMs)
                .send()
                .compose(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        final String errorMsg = String.format(
                                "data plane pull failed [status=%d, endpoint=%s]",
                                response.statusCode(), edr.endpoint());
                        LOG.warn(errorMsg);
                        return Future.failedFuture(new EdcClientException(errorMsg, response.statusCode()));
                    }

                    final var body = response.body();
                    if (body == null || body.length() == 0) {
                        LOG.warn("empty response from data plane [endpoint={}]", edr.endpoint());
                        return Future.failedFuture(new EdcClientException(
                                "empty response from data plane", 200));
                    }

                    final String contentType = response.getHeader("Content-Type") != null
                            ? response.getHeader("Content-Type")
                            : "application/octet-stream";

                    LOG.debug("telemetry pulled [endpoint={}, contentType={}, size={}]",
                            edr.endpoint(), contentType, body.length());

                    return Future.succeededFuture(new TelemetryPullResult(body.getBytes(), contentType));
                });
    }
}
