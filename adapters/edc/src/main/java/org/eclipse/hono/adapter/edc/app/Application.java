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
package org.eclipse.hono.adapter.edc.app;

import org.eclipse.hono.adapter.AbstractProtocolAdapterApplication;
import org.eclipse.hono.adapter.edc.EdcAdapterMetrics;
import org.eclipse.hono.adapter.edc.EdcAdapterProperties;
import org.eclipse.hono.adapter.edc.EdcProtocolAdapter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * The Hono EDC adapter main application class.
 */
@ApplicationScoped
public class Application extends AbstractProtocolAdapterApplication<EdcAdapterProperties> {

    @Inject
    EdcAdapterMetrics metrics;

    /**
     * {@inheritDoc}
     * <p>
     * Creates a new {@link EdcProtocolAdapter} instance, configures it with the
     * adapter properties and metrics, and sets up the required service collaborators.
     */
    @Override
    protected EdcProtocolAdapter adapter() {
        final EdcProtocolAdapter adapter = new EdcProtocolAdapter();
        adapter.setConfig(protocolAdapterProperties);
        adapter.setMetrics(metrics);
        setCollaborators(adapter);
        return adapter;
    }
}
