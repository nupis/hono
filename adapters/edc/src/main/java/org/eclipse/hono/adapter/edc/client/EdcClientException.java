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
package org.eclipse.hono.adapter.edc.client;

/**
 * Exception thrown when the EDC Management API returns an error response.
 */
public class EdcClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final int statusCode;

    /**
     * Creates a new EdcClientException.
     *
     * @param message The error message.
     * @param statusCode The HTTP status code from the EDC API.
     */
    public EdcClientException(final String message, final int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Gets the HTTP status code from the EDC API response.
     *
     * @return The status code.
     */
    public int getStatusCode() {
        return statusCode;
    }
}
