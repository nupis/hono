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

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import io.vertx.core.Vertx;

/**
 * Tests for {@link EdcManagementClient} using WireMock to simulate the EDC Management API v3.
 */
class EdcManagementClientTest {

    private static final String API_KEY = "test-api-key";
    private static final String PROVIDER_DSP_URL = "https://provider:8282/api/dsp";
    private static final String PROVIDER_BPN = "BPNL00000003CRHK";

    private WireMockServer wireMock;
    private Vertx vertx;
    private EdcManagementClient client;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
        vertx = Vertx.vertx();

        final String managementApiUrl = "http://localhost:" + wireMock.port();
        client = new EdcManagementClient(vertx, managementApiUrl, API_KEY);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
        vertx.close();
    }

    @Test
    void queryCatalogReturnsParsedCatalogAssets() throws Exception {
        final String catalogResponseBody = """
                {
                  "@context": { "edc": "https://w3id.org/edc/v0.0.1/ns/" },
                  "@type": "dcat:Catalog",
                  "dcat:dataset": [
                    {
                      "@id": "sensor-001",
                      "odrl:hasPolicy": {
                        "@id": "offer-abc-123",
                        "@type": "odrl:Offer",
                        "odrl:permission": [],
                        "odrl:prohibition": [],
                        "odrl:obligation": []
                      }
                    },
                    {
                      "@id": "sensor-002",
                      "odrl:hasPolicy": {
                        "@id": "offer-def-456",
                        "@type": "odrl:Offer",
                        "odrl:permission": [],
                        "odrl:prohibition": [],
                        "odrl:obligation": []
                      }
                    }
                  ]
                }
                """;

        wireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/management/v3/catalog/request"))
                .withHeader("X-Api-Key", WireMock.equalTo(API_KEY))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(catalogResponseBody)));

        final var assets = client.queryCatalog(PROVIDER_DSP_URL, PROVIDER_BPN)
                .toCompletionStage().toCompletableFuture()
                .get(5, TimeUnit.SECONDS);

        Assertions.assertEquals(2, assets.size());
        Assertions.assertEquals("sensor-001", assets.get(0).assetId());
        Assertions.assertEquals("offer-abc-123", assets.get(0).offerId());
        Assertions.assertEquals("sensor-002", assets.get(1).assetId());
        Assertions.assertEquals("offer-def-456", assets.get(1).offerId());
    }

    @Test
    void initiateNegotiationSendsCorrectJsonLdRequest() throws Exception {
        final String idResponseBody = """
                {
                  "@type": "IdResponse",
                  "@id": "negotiation-id-xyz",
                  "edc:createdAt": 1709920000000
                }
                """;

        wireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/management/v3/contractnegotiations"))
                .withHeader("X-Api-Key", WireMock.equalTo(API_KEY))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(idResponseBody)));

        final var result = client.initiateNegotiation(PROVIDER_DSP_URL, PROVIDER_BPN, "offer-abc-123", "sensor-001")
                .toCompletionStage().toCompletableFuture()
                .get(5, TimeUnit.SECONDS);

        Assertions.assertEquals("negotiation-id-xyz", result);

        wireMock.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/management/v3/contractnegotiations"))
                .withRequestBody(WireMock.equalToJson("""
                        {
                          "@context": { "edc": "https://w3id.org/edc/v0.0.1/ns/" },
                          "@type": "ContractRequest",
                          "counterPartyAddress": "%s",
                          "counterPartyId": "%s",
                          "protocol": "dataspace-protocol-http",
                          "policy": {
                            "@id": "offer-abc-123",
                            "@type": "odrl:Offer",
                            "odrl:permission": [],
                            "odrl:prohibition": [],
                            "odrl:obligation": [],
                            "odrl:target": "sensor-001"
                          }
                        }
                        """.formatted(PROVIDER_DSP_URL, PROVIDER_BPN), true, false)));
    }

    @Test
    void getNegotiationStateReturnsFinalizedWithAgreementId() throws Exception {
        wireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/management/v3/contractnegotiations/negotiation-id-xyz"))
                .withHeader("X-Api-Key", WireMock.equalTo(API_KEY))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "@type": "ContractNegotiation",
                                  "@id": "negotiation-id-xyz",
                                  "edc:state": "FINALIZED",
                                  "edc:contractAgreementId": "agreement-id-456"
                                }
                                """)));

        final var state = client.getNegotiationState("negotiation-id-xyz")
                .toCompletionStage().toCompletableFuture()
                .get(5, TimeUnit.SECONDS);

        Assertions.assertTrue(state.isFinalized());
        Assertions.assertEquals("agreement-id-456", state.contractAgreementId());
        Assertions.assertEquals("negotiation-id-xyz", state.id());
    }

    @Test
    void getNegotiationStateReturnsTerminatedOnFailure() throws Exception {
        wireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/management/v3/contractnegotiations/negotiation-id-fail"))
                .withHeader("X-Api-Key", WireMock.equalTo(API_KEY))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "@type": "ContractNegotiation",
                                  "@id": "negotiation-id-fail",
                                  "edc:state": "TERMINATED"
                                }
                                """)));

        final var state = client.getNegotiationState("negotiation-id-fail")
                .toCompletionStage().toCompletableFuture()
                .get(5, TimeUnit.SECONDS);

        Assertions.assertTrue(state.isTerminated());
        Assertions.assertTrue(state.isTerminal());
    }

    @Test
    void initiateTransferSendsCorrectRequestWithAgreementId() throws Exception {
        final String idResponseBody = """
                {
                  "@type": "IdResponse",
                  "@id": "transfer-id-789",
                  "edc:createdAt": 1709920000000
                }
                """;

        wireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/management/v3/transferprocesses"))
                .withHeader("X-Api-Key", WireMock.equalTo(API_KEY))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(idResponseBody)));

        final var result = client.initiateTransfer(PROVIDER_DSP_URL, PROVIDER_BPN, "agreement-id-456")
                .toCompletionStage().toCompletableFuture()
                .get(5, TimeUnit.SECONDS);

        Assertions.assertEquals("transfer-id-789", result);

        wireMock.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/management/v3/transferprocesses"))
                .withRequestBody(WireMock.equalToJson("""
                        {
                          "@context": { "edc": "https://w3id.org/edc/v0.0.1/ns/" },
                          "@type": "TransferRequestDto",
                          "connectorId": "%s",
                          "counterPartyAddress": "%s",
                          "contractAgreementId": "agreement-id-456",
                          "protocol": "dataspace-protocol-http",
                          "transferType": "HttpData-PULL",
                          "dataDestination": {
                            "@type": "DataAddress",
                            "type": "HttpProxy"
                          }
                        }
                        """.formatted(PROVIDER_BPN, PROVIDER_DSP_URL), true, false)));
    }

    @Test
    void getTransferStateReturnsStarted() throws Exception {
        wireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/management/v3/transferprocesses/transfer-id-789"))
                .withHeader("X-Api-Key", WireMock.equalTo(API_KEY))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "@type": "TransferProcess",
                                  "@id": "transfer-id-789",
                                  "edc:state": "STARTED"
                                }
                                """)));

        final var state = client.getTransferState("transfer-id-789")
                .toCompletionStage().toCompletableFuture()
                .get(5, TimeUnit.SECONDS);

        Assertions.assertTrue(state.isStarted());
        Assertions.assertEquals("transfer-id-789", state.id());
    }

    @Test
    void getEndpointDataReferenceReturnsEdrWithEndpointAndAuth() throws Exception {
        wireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/management/v3/edrs/transfer-id-789/dataaddress"))
                .withHeader("X-Api-Key", WireMock.equalTo(API_KEY))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "@type": "DataAddress",
                                  "edc:type": "https://w3id.org/idsa/v4.1/HTTP",
                                  "edc:endpoint": "https://provider-dataplane:8080/api/public",
                                  "edc:authorization": "eyJhbGciOiJSUzI1NiJ9.test-token"
                                }
                                """)));

        final var edr = client.getEndpointDataReference("transfer-id-789")
                .toCompletionStage().toCompletableFuture()
                .get(5, TimeUnit.SECONDS);

        Assertions.assertNotNull(edr);
        Assertions.assertEquals("https://provider-dataplane:8080/api/public", edr.endpoint());
        Assertions.assertEquals("eyJhbGciOiJSUzI1NiJ9.test-token", edr.authorization());
    }

    @Test
    void queryCatalogHandles5xxError() throws Exception {
        wireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/management/v3/catalog/request"))
                .willReturn(WireMock.aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        final var future = client.queryCatalog(PROVIDER_DSP_URL, PROVIDER_BPN)
                .toCompletionStage().toCompletableFuture();

        Assertions.assertTrue(future.isCompletedExceptionally() || assertThrowsOnGet(future));
    }

    @Test
    void initiateNegotiationHandles4xxError() throws Exception {
        wireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/management/v3/contractnegotiations"))
                .willReturn(WireMock.aResponse()
                        .withStatus(400)
                        .withBody("Bad Request")));

        final var future = client.initiateNegotiation(PROVIDER_DSP_URL, PROVIDER_BPN, "bad-offer", "bad-asset")
                .toCompletionStage().toCompletableFuture();

        Assertions.assertTrue(future.isCompletedExceptionally() || assertThrowsOnGet(future));
    }

    private boolean assertThrowsOnGet(final java.util.concurrent.CompletableFuture<?> future) {
        try {
            future.get(5, TimeUnit.SECONDS);
            return false;
        } catch (final Exception e) {
            return true;
        }
    }
}
