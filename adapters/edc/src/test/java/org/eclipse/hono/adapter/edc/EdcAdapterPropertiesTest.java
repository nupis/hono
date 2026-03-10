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
package org.eclipse.hono.adapter.edc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EdcAdapterProperties}.
 */
class EdcAdapterPropertiesTest {

    @Test
    void testAssetIdFilterDefaultsToNull() {
        final var props = new EdcAdapterProperties();
        assertNull(props.getAssetIdFilter());
    }

    @Test
    void testSetAssetIdFilterReturnsValue() {
        final var props = new EdcAdapterProperties();
        props.setAssetIdFilter("sensor-042");
        assertEquals("sensor-042", props.getAssetIdFilter());
    }

    @Test
    void testSetAssetIdFilterTrimsWhitespace() {
        final var props = new EdcAdapterProperties();
        props.setAssetIdFilter("  sensor-042  ");
        assertEquals("sensor-042", props.getAssetIdFilter());
    }

    @Test
    void testSetAssetIdFilterBlankTreatedAsNull() {
        final var props = new EdcAdapterProperties();
        props.setAssetIdFilter("   ");
        assertNull(props.getAssetIdFilter());
    }

    @Test
    void testSetAssetIdFilterEmptyTreatedAsNull() {
        final var props = new EdcAdapterProperties();
        props.setAssetIdFilter("");
        assertNull(props.getAssetIdFilter());
    }

    @Test
    void testSetAssetIdFilterNullSetsNull() {
        final var props = new EdcAdapterProperties();
        props.setAssetIdFilter("sensor-042");
        props.setAssetIdFilter(null);
        assertNull(props.getAssetIdFilter());
    }
}
