# Research: Add Provider BPN

**Branch**: `004-add-provider-bpn` | **Date**: 2026-03-10

## R1: EDC Management API — Where BPN Is Required

**Decision**: Add provider BPN (`counterPartyId` / `connectorId`) to three API request models: `CatalogRequest`, `ContractNegotiationRequest`, and `TransferRequest`.

**Rationale**: The EDC Management API v3 uses the BPN as the participant identifier in the Dataspace Protocol. Specifically:

| API Call | Field Name | Current Value | Required Value |
|----------|------------|---------------|----------------|
| Catalog Query (`CatalogRequest`) | `counterPartyId` | *missing* | Provider BPN |
| Contract Negotiation (`ContractNegotiationRequest`) | `counterPartyId` | *missing* | Provider BPN |
| Transfer Request (`TransferRequest`) | `connectorId` | `"provider"` (hardcoded) | Provider BPN |

In production EDC environments (e.g., Catena-X), the `counterPartyId` is validated against the BPN in the provider's Verifiable Credential. Missing or incorrect BPNs cause catalog queries to return empty results and negotiations to be rejected.

**Alternatives considered**:
- Extract BPN from EDC catalog response → Rejected: the BPN must be known *before* the catalog query to identify the counter-party.
- Per-asset BPN (different providers per asset) → Rejected: the adapter is designed for a single provider per instance. Multi-provider support would be a separate feature.

## R2: Configuration Property Design

**Decision**: Add a required property `hono.edc.providerBpn` (environment variable: `HONO_EDC_PROVIDER_BPN`). Type: `String`, required, non-blank.

**Rationale**: Follows the existing naming convention (`hono.edc.*`). The BPN is required for all EDC interactions, so it must be validated at startup — unlike `assetIdFilter` which is optional. The property is passed through the same config chain: `EdcAdapterOptions` → `EdcAdapterProperties` → `EdcManagementClient`.

**Alternatives considered**:
- Optional BPN with fallback to "provider" → Rejected: a placeholder BPN will fail in production EDC environments. Better to fail fast at startup.
- Embed BPN in `providerDspUrl` → Rejected: BPN and DSP URL are independent identifiers (a provider can change its DSP URL while keeping its BPN).

## R3: BPN Propagation Path

**Decision**: Pass the BPN from `EdcAdapterProperties` through `EdcManagementClient` to each request model's factory method.

**Rationale**: The `EdcManagementClient` already receives `providerDspUrl` as a method parameter on `queryCatalog()`, `initiateNegotiation()`, and `initiateTransfer()`. Adding `providerBpn` as an additional parameter to these methods follows the same pattern. The request model `create()` factory methods are updated to accept the BPN.

**Flow**:
```
EdcAdapterProperties.getProviderBpn()
  → TelemetryPollingService passes to EdcManagementClient methods
    → EdcManagementClient passes to request model factory methods
      → CatalogRequest.create(providerDspUrl, providerBpn, ...)
      → ContractNegotiationRequest.create(providerDspUrl, providerBpn, ...)
      → TransferRequest.create(providerDspUrl, providerBpn, ...)
```

**Alternatives considered**:
- Store BPN in `EdcManagementClient` as a constructor parameter → Viable and actually cleaner since the BPN is static per adapter instance. However, for consistency with `providerDspUrl` which is already passed per-method, we follow the same pattern. **Note**: If this becomes cumbersome, refactoring both `providerDspUrl` and `providerBpn` into the client constructor would be a reasonable follow-up.
- Inject BPN into request models via a middleware/interceptor → Rejected: over-engineering for a simple parameter pass-through.

## R4: JSON-LD Field Names for BPN

**Decision**: Use `counterPartyId` for catalog and negotiation requests, `connectorId` for transfer requests.

**Rationale**: The EDC Management API v3 uses different field names in different request types:
- `CatalogRequest`: `counterPartyId` — identifies who you're querying the catalog from
- `ContractRequest`: `counterPartyId` — identifies who you're negotiating with
- `TransferRequestDto`: `connectorId` — identifies the connector to transfer from

These are the standard field names in EDC 0.16.x Management API. The value in all three cases is the provider's BPN.

## R5: Startup Validation

**Decision**: Validate `providerBpn` in `EdcProtocolAdapter.doStart()` alongside the existing `managementApiUrl` and `managementApiKey` checks.

**Rationale**: The adapter already validates required configuration at startup (line 125-129 of `EdcProtocolAdapter.java`). Adding `providerBpn` to this check follows the established pattern and provides a clear, early error message.
