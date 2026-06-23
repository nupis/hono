# EDC Management API Contract: BPN Changes

**Branch**: `004-add-provider-bpn` | **Date**: 2026-03-10
**EDC API Version**: Management API v3 (EDC 0.16.x)

## Summary

This document describes the changes to EDC Management API request payloads to include the provider's BPN.

## 1. Catalog Query — CHANGED

**POST** `{managementApiUrl}/management/v3/catalog/request`

### Before
```json
{
  "@context": { "edc": "https://w3id.org/edc/v0.0.1/ns/" },
  "@type": "CatalogRequest",
  "counterPartyAddress": "{providerDspUrl}",
  "protocol": "dataspace-protocol-http",
  "querySpec": { "offset": 0, "limit": 100 }
}
```

### After
```json
{
  "@context": { "edc": "https://w3id.org/edc/v0.0.1/ns/" },
  "@type": "CatalogRequest",
  "counterPartyAddress": "{providerDspUrl}",
  "counterPartyId": "{providerBpn}",
  "protocol": "dataspace-protocol-http",
  "querySpec": { "offset": 0, "limit": 100 }
}
```

**Change**: Added `counterPartyId` field with provider BPN value.

## 2. Contract Negotiation — CHANGED

**POST** `{managementApiUrl}/management/v3/contractnegotiations`

### Before
```json
{
  "@context": { "edc": "https://w3id.org/edc/v0.0.1/ns/" },
  "@type": "ContractRequest",
  "counterPartyAddress": "{providerDspUrl}",
  "protocol": "dataspace-protocol-http",
  "policy": { ... }
}
```

### After
```json
{
  "@context": { "edc": "https://w3id.org/edc/v0.0.1/ns/" },
  "@type": "ContractRequest",
  "counterPartyAddress": "{providerDspUrl}",
  "counterPartyId": "{providerBpn}",
  "protocol": "dataspace-protocol-http",
  "policy": { ... }
}
```

**Change**: Added `counterPartyId` field with provider BPN value.

## 3. Transfer Process — CHANGED

**POST** `{managementApiUrl}/management/v3/transferprocesses`

### Before
```json
{
  "@context": { "edc": "https://w3id.org/edc/v0.0.1/ns/" },
  "@type": "TransferRequestDto",
  "connectorId": "provider",
  "counterPartyAddress": "{providerDspUrl}",
  "contractAgreementId": "{contractAgreementId}",
  "protocol": "dataspace-protocol-http",
  "transferType": "HttpData-PULL",
  "dataDestination": { "@type": "DataAddress", "type": "HttpProxy" }
}
```

### After
```json
{
  "@context": { "edc": "https://w3id.org/edc/v0.0.1/ns/" },
  "@type": "TransferRequestDto",
  "connectorId": "{providerBpn}",
  "counterPartyAddress": "{providerDspUrl}",
  "contractAgreementId": "{contractAgreementId}",
  "protocol": "dataspace-protocol-http",
  "transferType": "HttpData-PULL",
  "dataDestination": { "@type": "DataAddress", "type": "HttpProxy" }
}
```

**Change**: `connectorId` changed from hardcoded `"provider"` to configured provider BPN.

## 4. Unchanged Endpoints

The following endpoints are **not affected** (they don't reference the provider identity):

- `GET /contractnegotiations/{id}` — state polling, no BPN needed
- `GET /transferprocesses/{id}` — state polling, no BPN needed
- `GET /edrs/{id}/dataaddress` — EDR retrieval, no BPN needed
- `GET {edr.endpoint}` — data plane pull, authenticated via EDR token
