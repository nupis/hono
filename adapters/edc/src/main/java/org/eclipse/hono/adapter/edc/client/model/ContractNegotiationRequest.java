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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A JSON-LD request to initiate a contract negotiation with an EDC provider.
 *
 * @param context The JSON-LD context mapping.
 * @param type The JSON-LD type, always "ContractRequest".
 * @param counterPartyAddress The provider DSP endpoint URL.
 * @param counterPartyId The provider's Business Partner Number.
 * @param protocol The dataspace protocol identifier.
 * @param policy The ODRL offer/policy to negotiate.
 */
public record ContractNegotiationRequest(
        @JsonProperty("@context") Map<String, String> context,
        @JsonProperty("@type") String type,
        String counterPartyAddress,
        String counterPartyId,
        String protocol,
        OfferPolicy policy) {

    /**
     * An ODRL offer policy for contract negotiation.
     *
     * @param id The offer/policy identifier.
     * @param type The ODRL type, always "odrl:Offer".
     * @param permission The ODRL permissions (typically empty for open policies).
     * @param prohibition The ODRL prohibitions (typically empty for open policies).
     * @param obligation The ODRL obligations (typically empty for open policies).
     * @param target The target asset identifier.
     */
    public record OfferPolicy(
            @JsonProperty("@id") String id,
            @JsonProperty("@type") String type,
            @JsonProperty("odrl:permission") List<Object> permission,
            @JsonProperty("odrl:prohibition") List<Object> prohibition,
            @JsonProperty("odrl:obligation") List<Object> obligation,
            @JsonProperty("odrl:target") String target) {
    }

    private static final Map<String, String> EDC_CONTEXT = Map.of(
            "edc", "https://w3id.org/edc/v0.0.1/ns/");

    /**
     * Creates a new ContractNegotiationRequest for the given asset and offer.
     *
     * @param counterPartyAddress The provider DSP endpoint URL.
     * @param providerBpn The provider's Business Partner Number.
     * @param offerId The ODRL offer/policy identifier from the catalog.
     * @param assetId The target asset identifier.
     * @return A new ContractNegotiationRequest instance.
     */
    public static ContractNegotiationRequest create(
            final String counterPartyAddress,
            final String providerBpn,
            final String offerId,
            final String assetId) {

        final var policy = new OfferPolicy(
                offerId,
                "odrl:Offer",
                List.of(),
                List.of(),
                List.of(),
                assetId);

        return new ContractNegotiationRequest(
                EDC_CONTEXT,
                "ContractRequest",
                counterPartyAddress,
                providerBpn,
                "dataspace-protocol-http",
                policy);
    }
}
