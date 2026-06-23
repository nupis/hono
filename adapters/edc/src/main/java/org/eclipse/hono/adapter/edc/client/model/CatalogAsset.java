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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A discovered asset from the EDC catalog, extracted from the DCAT catalog response.
 * <p>
 * Each CatalogAsset represents a single dataset/asset with its associated ODRL offer ID,
 * which is needed for contract negotiation. The assetId corresponds to a Hono device ID.
 *
 * @param assetId The asset identifier (= Hono device ID).
 * @param offerId The ODRL offer/policy ID for contract negotiation.
 * @param providerDspUrl The provider's DSP endpoint URL.
 */
public record CatalogAsset(String assetId, String offerId, String providerDspUrl) {

    /**
     * Creates a CatalogAsset with validation.
     *
     * @param assetId The asset identifier.
     * @param offerId The ODRL offer/policy ID.
     * @param providerDspUrl The provider's DSP endpoint URL.
     */
    public CatalogAsset {
        Objects.requireNonNull(assetId, "assetId must not be null");
        Objects.requireNonNull(offerId, "offerId must not be null");
        Objects.requireNonNull(providerDspUrl, "providerDspUrl must not be null");
    }

    /**
     * Extracts CatalogAsset instances from a CatalogResponse.
     *
     * @param response The catalog response from the EDC Management API.
     * @param providerDspUrl The provider's DSP endpoint URL.
     * @return An unmodifiable list of CatalogAsset instances.
     */
    public static List<CatalogAsset> fromCatalogResponse(
            final CatalogResponse response,
            final String providerDspUrl) {

        if (response == null || response.datasets() == null) {
            return List.of();
        }

        final List<CatalogAsset> assets = new ArrayList<>();
        for (final var dataset : response.datasets()) {
            if (dataset.id() != null && dataset.hasPolicy() != null && dataset.hasPolicy().id() != null) {
                assets.add(new CatalogAsset(dataset.id(), dataset.hasPolicy().id(), providerDspUrl));
            }
        }
        return Collections.unmodifiableList(assets);
    }
}
