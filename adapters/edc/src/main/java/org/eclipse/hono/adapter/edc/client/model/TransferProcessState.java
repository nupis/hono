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
 * The state of a transfer process as returned by the EDC Management API.
 *
 * @param type The JSON-LD type, expected to be "TransferProcess".
 * @param id The EDC-assigned transfer process identifier.
 * @param state The current transfer state (REQUESTED, PROVISIONED, STARTED, COMPLETED, TERMINATED).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TransferProcessState(
        @JsonProperty("@type") String type,
        @JsonProperty("@id") String id,
        @JsonProperty("edc:state") String state) {

    /**
     * Checks whether the transfer has reached the STARTED state (EDR available).
     *
     * @return {@code true} if the state is STARTED.
     */
    public boolean isStarted() {
        return "STARTED".equals(state);
    }

    /**
     * Checks whether the transfer has been terminated (failure).
     *
     * @return {@code true} if the state is TERMINATED.
     */
    public boolean isTerminated() {
        return "TERMINATED".equals(state);
    }

    /**
     * Checks whether the transfer has reached a terminal state or is ready for data pull.
     *
     * @return {@code true} if the state is STARTED, COMPLETED, or TERMINATED.
     */
    public boolean isTerminalOrReady() {
        return isStarted() || "COMPLETED".equals(state) || isTerminated();
    }
}
