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
import java.util.Optional;

import org.eclipse.hono.adapter.ProtocolAdapterOptions;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigMapping.NamingStrategy;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

/**
 * Options for configuring the EDC protocol adapter.
 */
@ConfigMapping(prefix = "hono.edc", namingStrategy = NamingStrategy.VERBATIM)
public interface EdcAdapterOptions {

    /**
     * Gets the adapter options.
     *
     * @return The options.
     */
    @WithParentName
    ProtocolAdapterOptions adapterOptions();

    /**
     * Gets the interval between global polling cycles.
     *
     * @return The polling interval.
     */
    @WithDefault("PT60S")
    Duration pollingInterval();

    /**
     * Gets the EDC Consumer Management API base URL.
     *
     * @return The URL.
     */
    Optional<String> managementApiUrl();

    /**
     * Gets the API key for EDC Management API authentication.
     *
     * @return The API key.
     */
    Optional<String> managementApiKey();

    /**
     * Gets the EDC Provider DSP endpoint URL.
     *
     * @return The URL.
     */
    Optional<String> providerDspUrl();

    /**
     * Gets the timeout for data plane HTTP pull requests.
     *
     * @return The timeout.
     */
    @WithDefault("PT30S")
    Duration dataPlaneRequestTimeout();

    /**
     * Gets the interval for polling contract negotiation state.
     *
     * @return The poll interval.
     */
    @WithDefault("PT1S")
    Duration negotiationPollInterval();

    /**
     * Gets the max time to wait for contract negotiation to finalize.
     *
     * @return The timeout.
     */
    @WithDefault("PT30S")
    Duration negotiationTimeout();

    /**
     * Gets the interval for polling transfer process state.
     *
     * @return The poll interval.
     */
    @WithDefault("PT1S")
    Duration transferPollInterval();

    /**
     * Gets the max time to wait for transfer to reach STARTED.
     *
     * @return The timeout.
     */
    @WithDefault("PT30S")
    Duration transferTimeout();

    /**
     * Gets the max retry attempts for transient EDC failures.
     *
     * @return The max retries.
     */
    @WithDefault("3")
    int maxRetries();

    /**
     * Gets the base duration for exponential backoff.
     *
     * @return The backoff base.
     */
    @WithDefault("PT1S")
    Duration retryBackoffBase();

    /**
     * Gets the optional asset ID filter for restricting polling to a single asset.
     *
     * @return The asset ID to filter on, or empty if no filter is configured.
     */
    Optional<String> assetIdFilter();
}
