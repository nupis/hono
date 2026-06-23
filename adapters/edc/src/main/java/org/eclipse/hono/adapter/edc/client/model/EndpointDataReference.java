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
package org.eclipse.hono.adapter.edc.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An Endpoint Data Reference (EDR) containing data plane access credentials.
 * <p>
 * Returned by the EDC Management API after a transfer process reaches the STARTED state.
 * Contains the data plane endpoint URL and authorization token for pulling telemetry data.
 * <p>
 * Note: The authorization token is sensitive and must never be logged.
 *
 * @param type The JSON-LD type, expected to be "DataAddress".
 * @param endpointType The endpoint type identifier.
 * @param endpoint The data plane public API URL.
 * @param authorization The bearer token for data plane access.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EndpointDataReference(
        @JsonProperty("@type") String type,
        @JsonProperty("edc:type") String endpointType,
        @JsonProperty("edc:endpoint") String endpoint,
        @JsonProperty("edc:authorization") String authorization) {
}
