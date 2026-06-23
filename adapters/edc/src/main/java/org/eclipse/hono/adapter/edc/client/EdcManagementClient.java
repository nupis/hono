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
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.hono.adapter.edc.client.model.CatalogAsset;
import org.eclipse.hono.adapter.edc.client.model.CatalogRequest;
import org.eclipse.hono.adapter.edc.client.model.CatalogResponse;
import org.eclipse.hono.adapter.edc.client.model.ContractNegotiationRequest;
import org.eclipse.hono.adapter.edc.client.model.ContractNegotiationState;
import org.eclipse.hono.adapter.edc.client.model.EndpointDataReference;
import org.eclipse.hono.adapter.edc.client.model.IdResponse;
import org.eclipse.hono.adapter.edc.client.model.TransferProcessState;
import org.eclipse.hono.adapter.edc.client.model.TransferRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * Client for the EDC Management API v3.
 * <p>
 * Provides methods for catalog queries, contract negotiation, transfer process management,
 * and endpoint data reference retrieval. Uses Vert.x WebClient for non-blocking I/O.
 */
public class EdcManagementClient {

    private static final Logger LOG = LoggerFactory.getLogger(EdcManagementClient.class);

    private static final String CATALOG_PATH = "/management/v3/catalog/request";
    private static final String NEGOTIATIONS_PATH = "/management/v3/contractnegotiations";
    private static final String TRANSFERS_PATH = "/management/v3/transferprocesses";
    private static final String EDRS_PATH = "/management/v3/edrs";
    private static final String API_KEY_HEADER = "X-Api-Key";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final int DEFAULT_CATALOG_LIMIT = 100;

    private final Vertx vertx;
    private final WebClient webClient;
    private final String managementApiUrl;
    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final int maxRetries;
    private final Duration retryBackoffBase;
    private final Duration negotiationPollInterval;
    private final Duration negotiationTimeout;
    private final Duration transferPollInterval;
    private final Duration transferTimeout;

    /**
     * Creates a new EdcManagementClient.
     *
     * @param vertx The Vert.x instance for async HTTP operations.
     * @param managementApiUrl The base URL of the EDC Management API.
     * @param apiKey The API key for authentication.
     */
    public EdcManagementClient(final Vertx vertx, final String managementApiUrl, final String apiKey) {
        this(vertx, managementApiUrl, apiKey, 3, Duration.ofSeconds(1),
                Duration.ofSeconds(1), Duration.ofSeconds(30),
                Duration.ofSeconds(1), Duration.ofSeconds(30));
    }

    /**
     * Creates a new EdcManagementClient with full configuration.
     *
     * @param vertx The Vert.x instance for async HTTP operations.
     * @param managementApiUrl The base URL of the EDC Management API.
     * @param apiKey The API key for authentication.
     * @param maxRetries Max retry attempts for transient failures.
     * @param retryBackoffBase Base duration for exponential backoff.
     * @param negotiationPollInterval Interval for polling negotiation state.
     * @param negotiationTimeout Max time to wait for negotiation to finalize.
     * @param transferPollInterval Interval for polling transfer state.
     * @param transferTimeout Max time to wait for transfer to start.
     */
    public EdcManagementClient(
            final Vertx vertx,
            final String managementApiUrl,
            final String apiKey,
            final int maxRetries,
            final Duration retryBackoffBase,
            final Duration negotiationPollInterval,
            final Duration negotiationTimeout,
            final Duration transferPollInterval,
            final Duration transferTimeout) {

        Objects.requireNonNull(vertx, "vertx must not be null");
        Objects.requireNonNull(managementApiUrl, "managementApiUrl must not be null");
        Objects.requireNonNull(apiKey, "apiKey must not be null");

        this.vertx = vertx;
        this.webClient = WebClient.create(vertx, new WebClientOptions());
        this.managementApiUrl = managementApiUrl;
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.maxRetries = maxRetries;
        this.retryBackoffBase = retryBackoffBase;
        this.negotiationPollInterval = negotiationPollInterval;
        this.negotiationTimeout = negotiationTimeout;
        this.transferPollInterval = transferPollInterval;
        this.transferTimeout = transferTimeout;
    }

    /**
     * Queries the EDC catalog for available assets.
     *
     * @param providerDspUrl The provider's DSP endpoint URL.
     * @param providerBpn The provider's Business Partner Number.
     * @return A future containing the list of discovered catalog assets.
     */
    public Future<List<CatalogAsset>> queryCatalog(final String providerDspUrl, final String providerBpn) {
        LOG.debug("querying EDC catalog [providerDspUrl={}, providerBpn={}]", providerDspUrl, providerBpn);

        final var request = CatalogRequest.create(providerDspUrl, providerBpn, 0, DEFAULT_CATALOG_LIMIT);
        return postJson(CATALOG_PATH, request)
                .compose(response -> checkStatus(response, "catalog query")
                        .compose(v -> deserialize(response.bodyAsBuffer(), CatalogResponse.class)))
                .map(catalogResponse -> {
                    final var assets = CatalogAsset.fromCatalogResponse(catalogResponse, providerDspUrl);
                    LOG.debug("discovered {} assets in EDC catalog", assets.size());
                    return assets;
                });
    }

    /**
     * Initiates a contract negotiation for the given asset and offer.
     *
     * @param providerDspUrl The provider's DSP endpoint URL.
     * @param providerBpn The provider's Business Partner Number.
     * @param offerId The ODRL offer/policy ID from the catalog.
     * @param assetId The target asset identifier.
     * @return A future containing the EDC-assigned negotiation ID.
     */
    public Future<String> initiateNegotiation(
            final String providerDspUrl,
            final String providerBpn,
            final String offerId,
            final String assetId) {

        LOG.debug("initiating contract negotiation [assetId={}, offerId={}, providerBpn={}]",
                assetId, offerId, providerBpn);

        final var request = ContractNegotiationRequest.create(providerDspUrl, providerBpn, offerId, assetId);
        return postJson(NEGOTIATIONS_PATH, request)
                .compose(response -> checkStatus(response, "initiate negotiation")
                        .compose(v -> deserialize(response.bodyAsBuffer(), IdResponse.class)))
                .map(idResponse -> {
                    LOG.debug("negotiation initiated [negotiationId={}]", idResponse.id());
                    return idResponse.id();
                });
    }

    /**
     * Gets the current state of a contract negotiation.
     *
     * @param negotiationId The EDC-assigned negotiation ID.
     * @return A future containing the negotiation state.
     */
    public Future<ContractNegotiationState> getNegotiationState(final String negotiationId) {
        LOG.trace("polling negotiation state [negotiationId={}]", negotiationId);

        return getJson(NEGOTIATIONS_PATH + "/" + negotiationId)
                .compose(response -> checkStatus(response, "get negotiation state")
                        .compose(v -> deserialize(response.bodyAsBuffer(), ContractNegotiationState.class)))
                .map(state -> {
                    LOG.debug("negotiation state [negotiationId={}, state={}]", negotiationId, state.state());
                    return state;
                });
    }

    /**
     * Initiates a data transfer process using the HttpData-PULL pattern.
     *
     * @param providerDspUrl The provider's DSP endpoint URL.
     * @param providerBpn The provider's Business Partner Number.
     * @param contractAgreementId The contract agreement ID from a finalized negotiation.
     * @return A future containing the EDC-assigned transfer process ID.
     */
    public Future<String> initiateTransfer(
            final String providerDspUrl,
            final String providerBpn,
            final String contractAgreementId) {

        LOG.debug("initiating transfer [contractAgreementId={}, providerBpn={}]",
                contractAgreementId, providerBpn);

        final var request = TransferRequest.create(providerDspUrl, providerBpn, contractAgreementId);
        return postJson(TRANSFERS_PATH, request)
                .compose(response -> checkStatus(response, "initiate transfer")
                        .compose(v -> deserialize(response.bodyAsBuffer(), IdResponse.class)))
                .map(idResponse -> {
                    LOG.debug("transfer initiated [transferProcessId={}]", idResponse.id());
                    return idResponse.id();
                });
    }

    /**
     * Gets the current state of a transfer process.
     *
     * @param transferProcessId The EDC-assigned transfer process ID.
     * @return A future containing the transfer process state.
     */
    public Future<TransferProcessState> getTransferState(final String transferProcessId) {
        LOG.trace("polling transfer state [transferProcessId={}]", transferProcessId);

        return getJson(TRANSFERS_PATH + "/" + transferProcessId)
                .compose(response -> checkStatus(response, "get transfer state")
                        .compose(v -> deserialize(response.bodyAsBuffer(), TransferProcessState.class)))
                .map(state -> {
                    LOG.debug("transfer state [transferProcessId={}, state={}]", transferProcessId, state.state());
                    return state;
                });
    }

    /**
     * Retrieves the Endpoint Data Reference (EDR) for a transfer process.
     *
     * @param transferProcessId The EDC-assigned transfer process ID.
     * @return A future containing the endpoint data reference with access credentials.
     */
    public Future<EndpointDataReference> getEndpointDataReference(final String transferProcessId) {
        LOG.debug("retrieving EDR [transferProcessId={}]", transferProcessId);

        return getJson(EDRS_PATH + "/" + transferProcessId + "/dataaddress")
                .compose(response -> checkStatus(response, "get EDR")
                        .compose(v -> deserialize(response.bodyAsBuffer(), EndpointDataReference.class)))
                .map(edr -> {
                    LOG.debug("EDR retrieved [transferProcessId={}, endpoint={}]",
                            transferProcessId, edr.endpoint());
                    return edr;
                });
    }

    /**
     * Polls the negotiation state until FINALIZED or TERMINATED, with timeout.
     *
     * @param negotiationId The EDC-assigned negotiation ID.
     * @return A future containing the contract agreement ID when FINALIZED.
     */
    public Future<String> awaitNegotiationFinalized(final String negotiationId) {
        LOG.debug("awaiting negotiation finalization [negotiationId={}]", negotiationId);

        final Promise<String> promise = Promise.promise();
        final long startTime = System.currentTimeMillis();
        final long timeoutMs = negotiationTimeout.toMillis();
        final long intervalMs = negotiationPollInterval.toMillis();

        pollNegotiation(negotiationId, promise, startTime, timeoutMs, intervalMs);
        return promise.future();
    }

    private void pollNegotiation(
            final String negotiationId,
            final Promise<String> promise,
            final long startTime,
            final long timeoutMs,
            final long intervalMs) {

        if (System.currentTimeMillis() - startTime > timeoutMs) {
            promise.fail(new EdcClientException(
                    "negotiation timed out [negotiationId=" + negotiationId + "]", 0));
            return;
        }

        getNegotiationState(negotiationId).onComplete(ar -> {
            if (ar.failed()) {
                promise.fail(ar.cause());
                return;
            }
            final var state = ar.result();
            if (state.isFinalized()) {
                LOG.info("negotiation finalized [negotiationId={}, agreementId={}]",
                        negotiationId, state.contractAgreementId());
                promise.complete(state.contractAgreementId());
            } else if (state.isTerminated()) {
                promise.fail(new EdcClientException(
                        "negotiation terminated [negotiationId=" + negotiationId + "]", 0));
            } else {
                vertx.setTimer(intervalMs, timerId ->
                        pollNegotiation(negotiationId, promise, startTime, timeoutMs, intervalMs));
            }
        });
    }

    /**
     * Polls the transfer state until STARTED or TERMINATED, with timeout.
     *
     * @param transferProcessId The EDC-assigned transfer process ID.
     * @return A future that succeeds when the transfer reaches STARTED.
     */
    public Future<Void> awaitTransferStarted(final String transferProcessId) {
        LOG.debug("awaiting transfer start [transferProcessId={}]", transferProcessId);

        final Promise<Void> promise = Promise.promise();
        final long startTime = System.currentTimeMillis();
        final long timeoutMs = transferTimeout.toMillis();
        final long intervalMs = transferPollInterval.toMillis();

        pollTransfer(transferProcessId, promise, startTime, timeoutMs, intervalMs);
        return promise.future();
    }

    private void pollTransfer(
            final String transferProcessId,
            final Promise<Void> promise,
            final long startTime,
            final long timeoutMs,
            final long intervalMs) {

        if (System.currentTimeMillis() - startTime > timeoutMs) {
            promise.fail(new EdcClientException(
                    "transfer timed out [transferProcessId=" + transferProcessId + "]", 0));
            return;
        }

        getTransferState(transferProcessId).onComplete(ar -> {
            if (ar.failed()) {
                promise.fail(ar.cause());
                return;
            }
            final var state = ar.result();
            if (state.isStarted()) {
                LOG.info("transfer started [transferProcessId={}]", transferProcessId);
                promise.complete();
            } else if (state.isTerminated()) {
                promise.fail(new EdcClientException(
                        "transfer terminated [transferProcessId=" + transferProcessId + "]", 0));
            } else {
                vertx.setTimer(intervalMs, timerId ->
                        pollTransfer(transferProcessId, promise, startTime, timeoutMs, intervalMs));
            }
        });
    }

    /**
     * Executes an operation with retry and exponential backoff for transient failures.
     * <p>
     * Retries on connection errors and 5xx HTTP errors. Does NOT retry on 4xx client errors.
     *
     * @param <T> The result type.
     * @param operation The async operation to execute.
     * @param operationName The operation name for logging.
     * @return A future containing the result.
     */
    public <T> Future<T> withRetry(final Supplier<Future<T>> operation, final String operationName) {
        return withRetry(operation, operationName, 0);
    }

    private <T> Future<T> withRetry(
            final Supplier<Future<T>> operation,
            final String operationName,
            final int attempt) {

        return operation.get().recover(error -> {
            if (attempt >= maxRetries || isClientError(error)) {
                return Future.failedFuture(error);
            }

            final long delayMs = retryBackoffBase.toMillis() * (1L << attempt);
            LOG.warn("retrying {} [attempt={}/{}, delayMs={}]",
                    operationName, attempt + 1, maxRetries, delayMs, error);

            final Promise<T> retryPromise = Promise.promise();
            vertx.setTimer(delayMs, timerId ->
                    withRetry(operation, operationName, attempt + 1)
                            .onComplete(retryPromise));
            return retryPromise.future();
        });
    }

    private boolean isClientError(final Throwable error) {
        if (error instanceof EdcClientException clientEx) {
            return clientEx.getStatusCode() >= 400 && clientEx.getStatusCode() < 500;
        }
        return false;
    }

    private Future<HttpResponse<Buffer>> postJson(final String path, final Object body) {
        try {
            final byte[] json = objectMapper.writeValueAsBytes(body);
            return webClient.requestAbs(HttpMethod.POST, managementApiUrl + path)
                    .putHeader(API_KEY_HEADER, apiKey)
                    .putHeader("Content-Type", CONTENT_TYPE_JSON)
                    .sendBuffer(Buffer.buffer(json));
        } catch (final Exception e) {
            return Future.failedFuture(e);
        }
    }

    private Future<HttpResponse<Buffer>> getJson(final String path) {
        return webClient.requestAbs(HttpMethod.GET, managementApiUrl + path)
                .putHeader(API_KEY_HEADER, apiKey)
                .putHeader("Accept", CONTENT_TYPE_JSON)
                .send();
    }

    private Future<Void> checkStatus(final HttpResponse<Buffer> response, final String operation) {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return Future.succeededFuture();
        }
        final String errorMsg = String.format(
                "EDC Management API error [operation=%s, status=%d, body=%s]",
                operation, response.statusCode(),
                response.bodyAsString() != null ? response.bodyAsString() : "empty");
        LOG.warn(errorMsg);
        return Future.failedFuture(new EdcClientException(errorMsg, response.statusCode()));
    }

    private <T> Future<T> deserialize(final Buffer buffer, final Class<T> type) {
        try {
            return Future.succeededFuture(objectMapper.readValue(buffer.getBytes(), type));
        } catch (final Exception e) {
            LOG.error("failed to deserialize EDC response to {}", type.getSimpleName(), e);
            return Future.failedFuture(e);
        }
    }
}
