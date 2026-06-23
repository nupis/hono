# Data Model: Add Provider BPN

**Branch**: `004-add-provider-bpn` | **Date**: 2026-03-10

## Overview

This feature adds a required configuration property (`providerBpn`) and propagates it into three existing EDC API request models. No new entities are introduced.

## Modified Entities

### EdcAdapterProperties (Configuration) — Extended

New required field added to the existing configuration.

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| providerBpn | String | **Yes** | — | The Business Partner Number (BPN) of the EDC Provider. Used as the counter-party/connector identifier in all EDC API requests. |

**Validation rules**:
- Must not be null or blank
- Trimmed of whitespace
- No format validation beyond non-blank (BPN formats vary across dataspaces)

**Configuration path**: `hono.edc.providerBpn`

**Environment variable**: `HONO_EDC_PROVIDER_BPN`

### CatalogRequest (Request Model) — Extended

New field: `counterPartyId`

| Field | Type | Description |
|-------|------|-------------|
| counterPartyId | String | **NEW** — The provider's BPN identifying the counter-party for catalog discovery |

**JSON-LD output** (updated):
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

### ContractNegotiationRequest (Request Model) — Extended

New field: `counterPartyId`

| Field | Type | Description |
|-------|------|-------------|
| counterPartyId | String | **NEW** — The provider's BPN identifying the counter-party for negotiation |

**JSON-LD output** (updated):
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

### TransferRequest (Request Model) — Changed

Existing field `connectorId` changes from hardcoded to configurable.

| Field | Type | Description |
|-------|------|-------------|
| connectorId | String | **CHANGED** — Was hardcoded to `"provider"`, now set to the configured provider BPN |

**JSON-LD output** (updated):
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

## Impact Assessment

| Component | Change Required |
|-----------|----------------|
| EdcAdapterOptions | Add `providerBpn()` method (required String) |
| EdcAdapterProperties | Add `providerBpn` field, getter, setter, validation |
| EdcProtocolAdapter | Add startup validation for `providerBpn` |
| EdcManagementClient | Add `providerBpn` parameter to `queryCatalog()`, `initiateNegotiation()`, `initiateTransfer()` |
| CatalogRequest | Add `counterPartyId` field and update `create()` factory |
| ContractNegotiationRequest | Add `counterPartyId` field and update `create()` factory |
| TransferRequest | Update `create()` factory to accept BPN instead of hardcoded "provider" |
| TelemetryPollingService | Pass `properties.getProviderBpn()` to management client methods |
| application.yaml | Add `providerBpn` property |
