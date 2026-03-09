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
 * A generic ID response from the EDC Management API.
 * <p>
 * Returned when initiating contract negotiations or transfer processes.
 *
 * @param type The JSON-LD type, expected to be "IdResponse".
 * @param id The EDC-assigned identifier.
 * @param createdAt The creation timestamp in milliseconds since epoch.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IdResponse(
        @JsonProperty("@type") String type,
        @JsonProperty("@id") String id,
        @JsonProperty("edc:createdAt") long createdAt) {
}
