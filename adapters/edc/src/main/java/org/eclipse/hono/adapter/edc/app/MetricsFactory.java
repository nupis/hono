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

import org.eclipse.hono.adapter.edc.EdcAdapterMetrics;
import org.eclipse.hono.adapter.edc.EdcAdapterOptions;
import org.eclipse.hono.adapter.edc.EdcAdapterProperties;
import org.eclipse.hono.adapter.edc.EdcProtocolAdapter;
import org.eclipse.hono.client.amqp.connection.SendMessageSampler;
import org.eclipse.hono.service.metric.MetricsTags;

import io.micrometer.core.instrument.config.MeterFilter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * A factory class that creates EDC protocol adapter specific metrics and properties.
 */
@ApplicationScoped
public class MetricsFactory {

    /**
     * Creates the EDC adapter properties from the injected configuration options.
     *
     * @param adapterOptions The SmallRye Config mapping for EDC adapter options.
     * @return The adapter properties.
     */
    @Singleton
    @Produces
    EdcAdapterProperties adapterProperties(final EdcAdapterOptions adapterOptions) {
        return new EdcAdapterProperties(adapterOptions);
    }

    /**
     * Creates a meter filter that applies common tags for the EDC protocol adapter.
     *
     * @return The meter filter.
     */
    @Produces
    @Singleton
    MeterFilter commonTags() {
        return MeterFilter.commonTags(MetricsTags.forProtocolAdapter(EdcProtocolAdapter.TYPE_NAME));
    }

    /**
     * Creates a no-op message sampler factory.
     * <p>
     * The EDC adapter does not send AMQP messages directly, so a no-op sampler is sufficient.
     *
     * @return The sampler factory.
     */
    @Singleton
    @Produces
    SendMessageSampler.Factory messageSamplerFactory() {
        return SendMessageSampler.Factory.noop();
    }

    /**
     * Creates the EDC adapter metrics instance.
     * <p>
     * Returns a no-op implementation by default. Override to provide a
     * Micrometer-based implementation when a {@link io.micrometer.core.instrument.MeterRegistry}
     * is available.
     *
     * @return The metrics instance.
     */
    @Singleton
    @Produces
    EdcAdapterMetrics metrics() {
        return EdcAdapterMetrics.NOOP;
    }
}
