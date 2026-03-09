# Data Model: EDC Data Ingestion

**Branch**: `001-edc-data-ingestion` | **Date**: 2026-03-09

## Overview

The EDC adapter is stateless — it does not persist data locally. All entities below are transient, existing only during a polling cycle. Hono's Device Registration and Tenant services are the authoritative data sources for device/tenant state.

## Entities

### EdcAdapterProperties (Configuration)

Configuration record for the EDC adapter, extending Hono's `ProtocolAdapterProperties`.

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| pollingInterval | Duration | Yes | 60s | Interval between global polling cycles |
| managementApiUrl | String | Yes | — | EDC Consumer Management API base URL |
| managementApiKey | String | Yes | — | API key for EDC Management API authentication |
| providerDspUrl | String | Yes | — | EDC Provider DSP endpoint URL (counterPartyAddress for catalog/negotiation) |
| dataPlaneRequestTimeout | Duration | No | 30s | Timeout for data plane HTTP pull requests |
| negotiationPollInterval | Duration | No | 1s | Interval for polling contract negotiation state |
| negotiationTimeout | Duration | No | 30s | Max time to wait for contract negotiation to finalize |
| transferPollInterval | Duration | No | 1s | Interval for polling transfer process state |
| transferTimeout | Duration | No | 30s | Max time to wait for transfer to reach STARTED |
| maxRetries | int | No | 3 | Max retry attempts for transient EDC failures |
| retryBackoffBase | Duration | No | 1s | Base duration for exponential backoff |

### CatalogAsset (Transient — from EDC catalog response)

Represents a single dataset/asset discovered in the EDC catalog.

| Field | Type | Description |
|-------|------|-------------|
| assetId | String | The asset identifier (= Hono device ID) |
| offerId | String | The ODRL offer/policy ID for contract negotiation |
| providerDspUrl | String | The provider's DSP endpoint (from catalog) |

### ContractNegotiationState (Transient — from EDC negotiation response)

Tracks the state of an in-progress contract negotiation.

| Field | Type | Description |
|-------|------|-------------|
| negotiationId | String | EDC-assigned negotiation ID |
| state | NegotiationState | Current state (REQUESTED, AGREED, VERIFIED, FINALIZED, TERMINATED) |
| contractAgreementId | String | Agreement ID (available when FINALIZED) |

**State transitions**:
```
REQUESTED → AGREED → VERIFIED → FINALIZED
                                     ↓
                              (agreement ID available)

REQUESTED → TERMINATED (on failure)
AGREED → TERMINATED (on failure)
```

### TransferProcessState (Transient — from EDC transfer response)

Tracks the state of a data transfer process.

| Field | Type | Description |
|-------|------|-------------|
| transferProcessId | String | EDC-assigned transfer process ID |
| state | TransferState | Current state (REQUESTED, PROVISIONED, STARTED, COMPLETED, TERMINATED) |

**State transitions**:
```
REQUESTED → PROVISIONED → STARTED → COMPLETED
                              ↓
                    (EDR available for data pull)

REQUESTED → TERMINATED (on failure)
```

### EndpointDataReference (Transient — from EDR cache)

Contains the data plane access credentials received after a successful transfer initiation.

| Field | Type | Description |
|-------|------|-------------|
| endpoint | String | Data plane public API URL |
| authorization | String | Bearer token for data plane access |
| contractId | String | Associated contract agreement ID |

### DeviceTelemetryResult (Transient — internal)

Represents fetched telemetry ready to forward into Hono.

| Field | Type | Description |
|-------|------|-------------|
| deviceId | String | Hono device ID (= EDC asset ID) |
| tenantId | String | Hono tenant ID (resolved from Device Registration API) |
| contentType | String | MIME type of the telemetry payload |
| payload | byte[] | Raw telemetry data from EDC data plane |
| correlationId | String | Correlation ID linking EDC transfer to Hono message |

## Relationships

```
EdcAdapterProperties
    ├── configures → TelemetryPollingService
    └── configures → EdcManagementClient

TelemetryPollingService (per polling cycle)
    ├── queries → EDC Catalog → [CatalogAsset]
    ├── for each CatalogAsset:
    │   ├── maps → DeviceAssetMapper → (deviceId, tenantId)
    │   ├── negotiates → ContractNegotiationState → contractAgreementId
    │   ├── transfers → TransferProcessState → transferProcessId
    │   ├── fetches EDR → EndpointDataReference
    │   ├── pulls data → DeviceTelemetryResult
    │   └── forwards → TelemetrySender → Hono messaging
    └── skips assets with no matching Hono device

DeviceAssetMapper
    ├── input: assetId (from EDC catalog)
    ├── lookup: DeviceRegistrationClient.assertRegistration(tenantId, deviceId)
    └── output: (tenantId, deviceId) or skip
```

## Validation Rules

- `managementApiUrl` must be a valid HTTPS URL
- `managementApiKey` must not be empty
- `providerDspUrl` must be a valid URL
- `pollingInterval` must be >= 10 seconds
- `assetId` from catalog must be non-empty and match a valid Hono device ID format
- Telemetry payload from data plane must be non-null and within size limits
- EDR `authorization` token must be present before data plane access
- `contractAgreementId` must be present before transfer initiation
