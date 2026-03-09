# EDC Management API Contract: Adapter Usage

**Branch**: `001-edc-data-ingestion` | **Date**: 2026-03-09
**EDC API Version**: Management API v3 (EDC 0.16.x)

## Authentication

All requests include:
```
X-Api-Key: {configured API key}
Content-Type: application/json
```

## 1. Catalog Query

**POST** `{managementApiUrl}/management/v3/catalog/request`

### Request
```json
{
  "@context": {
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "CatalogRequest",
  "counterPartyAddress": "{providerDspUrl}",
  "protocol": "dataspace-protocol-http",
  "querySpec": {
    "offset": 0,
    "limit": 100
  }
}
```

### Response (DCAT Catalog)
```json
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
    }
  ]
}
```

**Extraction**: For each `dcat:dataset`, extract `@id` as `assetId` and `odrl:hasPolicy.@id` as `offerId`.

## 2. Contract Negotiation

### Initiate

**POST** `{managementApiUrl}/management/v3/contractnegotiations`

```json
{
  "@context": {
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "ContractRequest",
  "counterPartyAddress": "{providerDspUrl}",
  "protocol": "dataspace-protocol-http",
  "policy": {
    "@id": "{offerId}",
    "@type": "odrl:Offer",
    "odrl:permission": [],
    "odrl:prohibition": [],
    "odrl:obligation": [],
    "odrl:target": "{assetId}"
  }
}
```

### Response
```json
{
  "@type": "IdResponse",
  "@id": "negotiation-id-xyz",
  "edc:createdAt": 1709920000000
}
```

### Poll State

**GET** `{managementApiUrl}/management/v3/contractnegotiations/{negotiationId}`

```json
{
  "@type": "ContractNegotiation",
  "@id": "negotiation-id-xyz",
  "edc:state": "FINALIZED",
  "edc:contractAgreementId": "agreement-id-456"
}
```

**Terminal states**: `FINALIZED` (success) or `TERMINATED` (failure).

## 3. Transfer Process

### Initiate

**POST** `{managementApiUrl}/management/v3/transferprocesses`

```json
{
  "@context": {
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "TransferRequestDto",
  "connectorId": "provider",
  "counterPartyAddress": "{providerDspUrl}",
  "contractAgreementId": "{contractAgreementId}",
  "protocol": "dataspace-protocol-http",
  "transferType": "HttpData-PULL",
  "dataDestination": {
    "@type": "DataAddress",
    "type": "HttpProxy"
  }
}
```

### Response
```json
{
  "@type": "IdResponse",
  "@id": "transfer-id-789",
  "edc:createdAt": 1709920000000
}
```

### Poll State

**GET** `{managementApiUrl}/management/v3/transferprocesses/{transferId}`

```json
{
  "@type": "TransferProcess",
  "@id": "transfer-id-789",
  "edc:state": "STARTED"
}
```

**Wait for**: `STARTED` state before retrieving EDR.

## 4. Endpoint Data Reference (EDR)

**GET** `{managementApiUrl}/management/v3/edrs/{transferProcessId}/dataaddress`

```json
{
  "@type": "DataAddress",
  "edc:type": "https://w3id.org/idsa/v4.1/HTTP",
  "edc:endpoint": "https://provider-dataplane:8080/api/public",
  "edc:authorization": "eyJhbGciOiJSUzI1NiJ9..."
}
```

## 5. Data Plane Pull

**GET** `{edr.endpoint}`

```
Authorization: {edr.authorization}
```

Response: raw telemetry payload (content type varies per provider).

## Complete Flow Per Device

```
1. POST /catalog/request          → discover assets
2. For each asset matching a Hono device:
   a. POST /contractnegotiations  → start negotiation
   b. GET  /contractnegotiations/{id} (poll until FINALIZED)
   c. POST /transferprocesses     → start transfer (HttpData-PULL)
   d. GET  /transferprocesses/{id} (poll until STARTED)
   e. GET  /edrs/{id}/dataaddress → get EDR
   f. GET  {edr.endpoint}         → pull telemetry data
   g. Forward to Hono via TelemetrySender
```

## Error Handling

| Scenario | HTTP Status | Adapter Behavior |
|----------|-------------|------------------|
| Catalog query fails | 4xx/5xx | Retry with backoff; abort cycle on persistent failure |
| Negotiation fails | state=TERMINATED | Log with correlation ID; skip device; continue to next |
| Transfer fails | state=TERMINATED | Log with correlation ID; skip device; continue to next |
| EDR not available | 404 | Retry after short delay; skip device after timeout |
| Data plane pull fails | 4xx/5xx | Retry with backoff; skip device after max retries |
| Management API unreachable | Connection error | Mark adapter readiness as unhealthy; retry next cycle |
