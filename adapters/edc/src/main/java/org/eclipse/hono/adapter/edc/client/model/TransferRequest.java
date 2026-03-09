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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A JSON-LD request to initiate a data transfer process via the EDC Management API.
 *
 * @param context The JSON-LD context mapping.
 * @param type The JSON-LD type, always "TransferRequestDto".
 * @param connectorId The connector identifier (typically "provider").
 * @param counterPartyAddress The provider DSP endpoint URL.
 * @param contractAgreementId The contract agreement ID from a finalized negotiation.
 * @param protocol The dataspace protocol identifier.
 * @param transferType The transfer type, always "HttpData-PULL".
 * @param dataDestination The data destination specification.
 */
public record TransferRequest(
        @JsonProperty("@context") Map<String, String> context,
        @JsonProperty("@type") String type,
        String connectorId,
        String counterPartyAddress,
        String contractAgreementId,
        String protocol,
        String transferType,
        DataDestination dataDestination) {

    /**
     * The data destination for an HttpData-PULL transfer.
     *
     * @param type The JSON-LD type, always "DataAddress".
     * @param destinationType The destination type, always "HttpProxy".
     */
    public record DataDestination(
            @JsonProperty("@type") String type,
            @JsonProperty("type") String destinationType) {
    }

    private static final Map<String, String> EDC_CONTEXT = Map.of(
            "edc", "https://w3id.org/edc/v0.0.1/ns/");

    /**
     * Creates a new TransferRequest for the given contract agreement.
     *
     * @param counterPartyAddress The provider DSP endpoint URL.
     * @param contractAgreementId The contract agreement ID from a finalized negotiation.
     * @return A new TransferRequest instance.
     */
    public static TransferRequest create(
            final String counterPartyAddress,
            final String contractAgreementId) {

        return new TransferRequest(
                EDC_CONTEXT,
                "TransferRequestDto",
                "provider",
                counterPartyAddress,
                contractAgreementId,
                "dataspace-protocol-http",
                "HttpData-PULL",
                new DataDestination("DataAddress", "HttpProxy"));
    }
}
