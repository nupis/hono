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
 * A JSON-LD request to query the EDC catalog.
 *
 * @param context The JSON-LD context mapping.
 * @param type The JSON-LD type, always "CatalogRequest".
 * @param counterPartyAddress The provider DSP endpoint URL.
 * @param protocol The dataspace protocol identifier.
 * @param querySpec The query specification with offset and limit.
 */
public record CatalogRequest(
        @JsonProperty("@context") Map<String, String> context,
        @JsonProperty("@type") String type,
        String counterPartyAddress,
        String protocol,
        QuerySpec querySpec) {

    /**
     * Query specification for catalog pagination.
     *
     * @param offset The result offset.
     * @param limit The maximum number of results.
     */
    public record QuerySpec(int offset, int limit) {
    }

    private static final Map<String, String> EDC_CONTEXT = Map.of(
            "edc", "https://w3id.org/edc/v0.0.1/ns/");

    /**
     * Creates a new CatalogRequest for the given provider.
     *
     * @param counterPartyAddress The provider DSP endpoint URL.
     * @param offset The result offset.
     * @param limit The maximum number of results.
     * @return A new CatalogRequest instance.
     */
    public static CatalogRequest create(final String counterPartyAddress, final int offset, final int limit) {
        return new CatalogRequest(
                EDC_CONTEXT,
                "CatalogRequest",
                counterPartyAddress,
                "dataspace-protocol-http",
                new QuerySpec(offset, limit));
    }
}
