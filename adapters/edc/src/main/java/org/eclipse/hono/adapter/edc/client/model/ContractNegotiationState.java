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
 * The state of a contract negotiation as returned by the EDC Management API.
 *
 * @param type The JSON-LD type, expected to be "ContractNegotiation".
 * @param id The EDC-assigned negotiation identifier.
 * @param state The current negotiation state (REQUESTED, AGREED, VERIFIED, FINALIZED, TERMINATED).
 * @param contractAgreementId The contract agreement ID, available when state is FINALIZED.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ContractNegotiationState(
        @JsonProperty("@type") String type,
        @JsonProperty("@id") String id,
        @JsonProperty("edc:state") String state,
        @JsonProperty("edc:contractAgreementId") String contractAgreementId) {

    /**
     * Checks whether the negotiation has reached the FINALIZED state.
     *
     * @return {@code true} if the state is FINALIZED.
     */
    public boolean isFinalized() {
        return "FINALIZED".equals(state);
    }

    /**
     * Checks whether the negotiation has been terminated (failure).
     *
     * @return {@code true} if the state is TERMINATED.
     */
    public boolean isTerminated() {
        return "TERMINATED".equals(state);
    }

    /**
     * Checks whether the negotiation has reached a terminal state.
     *
     * @return {@code true} if the state is FINALIZED or TERMINATED.
     */
    public boolean isTerminal() {
        return isFinalized() || isTerminated();
    }
}
