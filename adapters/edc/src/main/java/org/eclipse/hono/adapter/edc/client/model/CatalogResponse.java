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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A JSON-LD response from the EDC catalog query containing DCAT datasets.
 *
 * @param context The JSON-LD context mapping.
 * @param type The JSON-LD type, expected to be "dcat:Catalog".
 * @param datasets The list of DCAT datasets (assets) in the catalog.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogResponse(
        @JsonProperty("@context") Map<String, String> context,
        @JsonProperty("@type") String type,
        @JsonProperty("dcat:dataset") List<Dataset> datasets) {

    /**
     * A DCAT dataset representing an EDC asset with its associated ODRL policy.
     *
     * @param id The asset identifier (maps to Hono device ID).
     * @param hasPolicy The ODRL offer/policy associated with this asset.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dataset(
            @JsonProperty("@id") String id,
            @JsonProperty("odrl:hasPolicy") Policy hasPolicy) {
    }

    /**
     * An ODRL policy/offer for a dataset.
     *
     * @param id The offer/policy identifier used in contract negotiation.
     * @param type The ODRL type, expected to be "odrl:Offer".
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Policy(
            @JsonProperty("@id") String id,
            @JsonProperty("@type") String type) {
    }
}
