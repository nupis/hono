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

import java.time.Duration;
import java.util.Objects;

import org.eclipse.hono.adapter.ProtocolAdapterProperties;

/**
 * Configuration properties for the EDC protocol adapter.
 */
public class EdcAdapterProperties extends ProtocolAdapterProperties {

    private static final Duration DEFAULT_POLLING_INTERVAL = Duration.ofSeconds(60);
    private static final Duration DEFAULT_DATA_PLANE_REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_NEGOTIATION_POLL_INTERVAL = Duration.ofSeconds(1);
    private static final Duration DEFAULT_NEGOTIATION_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_TRANSFER_POLL_INTERVAL = Duration.ofSeconds(1);
    private static final Duration DEFAULT_TRANSFER_TIMEOUT = Duration.ofSeconds(30);
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final Duration DEFAULT_RETRY_BACKOFF_BASE = Duration.ofSeconds(1);
    private static final Duration MIN_POLLING_INTERVAL = Duration.ofSeconds(10);

    private Duration pollingInterval = DEFAULT_POLLING_INTERVAL;
    private String managementApiUrl;
    private String managementApiKey;
    private String providerDspUrl;
    private Duration dataPlaneRequestTimeout = DEFAULT_DATA_PLANE_REQUEST_TIMEOUT;
    private Duration negotiationPollInterval = DEFAULT_NEGOTIATION_POLL_INTERVAL;
    private Duration negotiationTimeout = DEFAULT_NEGOTIATION_TIMEOUT;
    private Duration transferPollInterval = DEFAULT_TRANSFER_POLL_INTERVAL;
    private Duration transferTimeout = DEFAULT_TRANSFER_TIMEOUT;
    private int maxRetries = DEFAULT_MAX_RETRIES;
    private Duration retryBackoffBase = DEFAULT_RETRY_BACKOFF_BASE;

    /**
     * Creates properties using default values.
     */
    public EdcAdapterProperties() {
        super();
    }

    /**
     * Creates properties using existing options.
     *
     * @param options The options to copy.
     */
    public EdcAdapterProperties(final EdcAdapterOptions options) {
        super(options.adapterOptions());
        this.pollingInterval = options.pollingInterval();
        options.managementApiUrl().ifPresent(url -> this.managementApiUrl = url);
        options.managementApiKey().ifPresent(key -> this.managementApiKey = key);
        options.providerDspUrl().ifPresent(url -> this.providerDspUrl = url);
        this.dataPlaneRequestTimeout = options.dataPlaneRequestTimeout();
        this.negotiationPollInterval = options.negotiationPollInterval();
        this.negotiationTimeout = options.negotiationTimeout();
        this.transferPollInterval = options.transferPollInterval();
        this.transferTimeout = options.transferTimeout();
        this.maxRetries = options.maxRetries();
        this.retryBackoffBase = options.retryBackoffBase();
    }

    /**
     * Gets the interval between global polling cycles.
     *
     * @return The polling interval.
     */
    public Duration getPollingInterval() {
        return pollingInterval;
    }

    /**
     * Sets the interval between global polling cycles.
     *
     * @param pollingInterval The polling interval.
     * @throws NullPointerException if pollingInterval is {@code null}.
     * @throws IllegalArgumentException if pollingInterval is less than 10 seconds.
     */
    public void setPollingInterval(final Duration pollingInterval) {
        Objects.requireNonNull(pollingInterval);
        if (pollingInterval.compareTo(MIN_POLLING_INTERVAL) < 0) {
            throw new IllegalArgumentException("polling interval must be at least 10 seconds");
        }
        this.pollingInterval = pollingInterval;
    }

    /**
     * Gets the EDC Consumer Management API base URL.
     *
     * @return The URL.
     */
    public String getManagementApiUrl() {
        return managementApiUrl;
    }

    /**
     * Sets the EDC Consumer Management API base URL.
     *
     * @param managementApiUrl The URL (must use HTTPS).
     * @throws NullPointerException if managementApiUrl is {@code null}.
     * @throws IllegalArgumentException if the URL does not use HTTPS.
     */
    public void setManagementApiUrl(final String managementApiUrl) {
        Objects.requireNonNull(managementApiUrl);
        if (!managementApiUrl.toLowerCase().startsWith("https://")) {
            throw new IllegalArgumentException("management API URL must use HTTPS");
        }
        this.managementApiUrl = managementApiUrl;
    }

    /**
     * Gets the API key for EDC Management API authentication.
     *
     * @return The API key.
     */
    public String getManagementApiKey() {
        return managementApiKey;
    }

    /**
     * Sets the API key for EDC Management API authentication.
     *
     * @param managementApiKey The API key.
     * @throws NullPointerException if managementApiKey is {@code null}.
     * @throws IllegalArgumentException if the API key is empty.
     */
    public void setManagementApiKey(final String managementApiKey) {
        Objects.requireNonNull(managementApiKey);
        if (managementApiKey.isBlank()) {
            throw new IllegalArgumentException("management API key must not be empty");
        }
        this.managementApiKey = managementApiKey;
    }

    /**
     * Gets the EDC Provider DSP endpoint URL.
     *
     * @return The URL.
     */
    public String getProviderDspUrl() {
        return providerDspUrl;
    }

    /**
     * Sets the EDC Provider DSP endpoint URL.
     *
     * @param providerDspUrl The URL.
     * @throws NullPointerException if providerDspUrl is {@code null}.
     */
    public void setProviderDspUrl(final String providerDspUrl) {
        this.providerDspUrl = Objects.requireNonNull(providerDspUrl);
    }

    /**
     * Gets the timeout for data plane HTTP pull requests.
     *
     * @return The timeout.
     */
    public Duration getDataPlaneRequestTimeout() {
        return dataPlaneRequestTimeout;
    }

    /**
     * Sets the timeout for data plane HTTP pull requests.
     *
     * @param dataPlaneRequestTimeout The timeout.
     * @throws NullPointerException if dataPlaneRequestTimeout is {@code null}.
     */
    public void setDataPlaneRequestTimeout(final Duration dataPlaneRequestTimeout) {
        this.dataPlaneRequestTimeout = Objects.requireNonNull(dataPlaneRequestTimeout);
    }

    /**
     * Gets the interval for polling contract negotiation state.
     *
     * @return The poll interval.
     */
    public Duration getNegotiationPollInterval() {
        return negotiationPollInterval;
    }

    /**
     * Sets the interval for polling contract negotiation state.
     *
     * @param negotiationPollInterval The poll interval.
     * @throws NullPointerException if negotiationPollInterval is {@code null}.
     */
    public void setNegotiationPollInterval(final Duration negotiationPollInterval) {
        this.negotiationPollInterval = Objects.requireNonNull(negotiationPollInterval);
    }

    /**
     * Gets the max time to wait for contract negotiation to finalize.
     *
     * @return The timeout.
     */
    public Duration getNegotiationTimeout() {
        return negotiationTimeout;
    }

    /**
     * Sets the max time to wait for contract negotiation to finalize.
     *
     * @param negotiationTimeout The timeout.
     * @throws NullPointerException if negotiationTimeout is {@code null}.
     */
    public void setNegotiationTimeout(final Duration negotiationTimeout) {
        this.negotiationTimeout = Objects.requireNonNull(negotiationTimeout);
    }

    /**
     * Gets the interval for polling transfer process state.
     *
     * @return The poll interval.
     */
    public Duration getTransferPollInterval() {
        return transferPollInterval;
    }

    /**
     * Sets the interval for polling transfer process state.
     *
     * @param transferPollInterval The poll interval.
     * @throws NullPointerException if transferPollInterval is {@code null}.
     */
    public void setTransferPollInterval(final Duration transferPollInterval) {
        this.transferPollInterval = Objects.requireNonNull(transferPollInterval);
    }

    /**
     * Gets the max time to wait for transfer to reach STARTED.
     *
     * @return The timeout.
     */
    public Duration getTransferTimeout() {
        return transferTimeout;
    }

    /**
     * Sets the max time to wait for transfer to reach STARTED.
     *
     * @param transferTimeout The timeout.
     * @throws NullPointerException if transferTimeout is {@code null}.
     */
    public void setTransferTimeout(final Duration transferTimeout) {
        this.transferTimeout = Objects.requireNonNull(transferTimeout);
    }

    /**
     * Gets the max retry attempts for transient EDC failures.
     *
     * @return The max retries.
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Sets the max retry attempts for transient EDC failures.
     *
     * @param maxRetries The max retries.
     * @throws IllegalArgumentException if maxRetries is negative.
     */
    public void setMaxRetries(final int maxRetries) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("max retries must be >= 0");
        }
        this.maxRetries = maxRetries;
    }

    /**
     * Gets the base duration for exponential backoff.
     *
     * @return The backoff base.
     */
    public Duration getRetryBackoffBase() {
        return retryBackoffBase;
    }

    /**
     * Sets the base duration for exponential backoff.
     *
     * @param retryBackoffBase The backoff base.
     * @throws NullPointerException if retryBackoffBase is {@code null}.
     */
    public void setRetryBackoffBase(final Duration retryBackoffBase) {
        this.retryBackoffBase = Objects.requireNonNull(retryBackoffBase);
    }
}
