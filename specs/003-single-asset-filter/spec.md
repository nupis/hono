# Feature Specification: Single Asset Filter

**Feature Branch**: `003-single-asset-filter`
**Created**: 2026-03-10
**Status**: Draft
**Input**: User description: "resume single asset filter"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Filter Polling to a Specific Asset (Priority: P1)

As a platform operator, I want to configure the EDC adapter to poll only a specific asset (by asset ID) instead of processing all assets discovered in the EDC catalog, so that I can target data ingestion to a single device and avoid unnecessary contract negotiations and data transfers for irrelevant assets.

**Why this priority**: This is the core value — without the ability to filter to a single asset, every polling cycle processes the entire catalog, which is wasteful and potentially costly (each asset triggers a fresh contract negotiation). Targeted polling is the minimum viable filtering capability.

**Independent Test**: Can be fully tested by configuring the adapter with a single asset ID filter, triggering a polling cycle against a catalog containing multiple assets, and verifying that only the specified asset's telemetry is fetched and forwarded.

**Acceptance Scenarios**:

1. **Given** the adapter is configured with an asset filter specifying asset ID "sensor-042", **When** a polling cycle runs against a catalog containing assets "sensor-001", "sensor-042", and "sensor-099", **Then** the adapter only negotiates a contract and fetches telemetry for "sensor-042".
2. **Given** the adapter is configured with an asset filter specifying asset ID "sensor-042", **When** a polling cycle runs and "sensor-042" is not found in the catalog, **Then** the adapter logs a warning indicating the filtered asset was not found and completes the cycle without errors.
3. **Given** the adapter is configured with an asset filter, **When** the filter configuration is changed to a different asset ID, **Then** the next polling cycle uses the updated filter without requiring a restart.

---

### User Story 2 - No Filter Configured (Backward Compatibility) (Priority: P1)

As a platform operator, I want the adapter to continue processing all catalog assets when no asset filter is configured, so that existing deployments are not affected by the introduction of filtering.

**Why this priority**: Backward compatibility is essential — existing users must not experience behavior changes when upgrading.

**Independent Test**: Can be tested by starting the adapter with no asset filter configured and verifying that all catalog assets are processed, matching the existing behavior.

**Acceptance Scenarios**:

1. **Given** the adapter has no asset filter configured, **When** a polling cycle runs, **Then** all assets in the EDC catalog are processed as before (existing behavior preserved).
2. **Given** the adapter has an empty or blank asset filter value, **When** the adapter starts, **Then** it treats this as "no filter" and processes all assets.

---

### User Story 3 - Operational Visibility of Active Filter (Priority: P2)

As a platform operator, I want to see which asset filter is active in the adapter's logs and health information, so that I can confirm the adapter is polling the intended asset and troubleshoot misconfigurations.

**Why this priority**: Visibility into active configuration is important for operations but not required for the core filtering to work.

**Independent Test**: Can be tested by configuring an asset filter, starting the adapter, and verifying that logs at startup and during polling cycles include the active filter information.

**Acceptance Scenarios**:

1. **Given** the adapter is configured with an asset filter for "sensor-042", **When** the adapter starts, **Then** it logs the active asset filter at startup.
2. **Given** the adapter is configured with an asset filter, **When** a polling cycle completes, **Then** the cycle summary log includes which asset was targeted and whether it was found.
3. **Given** no asset filter is configured, **When** the adapter starts, **Then** it logs that no filter is active and all assets will be processed.

---

### Edge Cases

- What happens when the configured asset ID contains invalid characters or exceeds length limits?
- What happens when the filtered asset exists in the catalog but has no corresponding device registered in Hono?
- What happens when the filtered asset's contract negotiation fails — does the adapter wait for the next cycle or retry immediately?
- What happens when the catalog query itself fails — is the filter error distinguishable from a catalog error in logs?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST support an optional configuration property to specify a single asset ID to filter on during polling cycles.
- **FR-002**: When an asset filter is configured, the system MUST only process the asset matching the specified ID from the EDC catalog, skipping all other assets.
- **FR-003**: When no asset filter is configured (absent, empty, or blank), the system MUST process all assets in the EDC catalog, preserving existing behavior.
- **FR-004**: System MUST log the active asset filter (or absence thereof) at adapter startup.
- **FR-005**: System MUST log a warning when a configured asset filter does not match any asset in the catalog during a polling cycle.
- **FR-006**: System MUST validate the asset filter value at startup and reject invalid values (empty after trimming, containing only whitespace) with a clear error message.
- **FR-007**: The asset filter MUST be applied after the catalog query and before contract negotiation, so that no unnecessary negotiations occur for non-matching assets.
- **FR-008**: System MUST include the active filter information in polling cycle log entries (correlation ID context) to support operational traceability.
- **FR-009**: System MUST support configuration changes to the asset filter without requiring adapter restart (hot-reload via standard configuration refresh).

### Key Entities

- **Asset Filter**: An optional configuration that restricts which EDC catalog asset is processed during polling. Contains a single asset ID value. When absent, all assets are processed.
- **Filtered Polling Cycle**: A polling cycle where catalog results are narrowed to the configured asset before further processing (negotiation, transfer, forwarding). All other cycle behavior remains unchanged.

## Assumptions

- The existing catalog query mechanism returns all available assets and filtering is applied in-memory after the query (the EDC Catalog API does not support server-side single-asset filtering).
- Asset IDs are stable identifiers that do not change between polling cycles for the same logical data source.
- Configuration hot-reload is supported by the underlying runtime (Quarkus configuration refresh).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: When an asset filter is configured, only 1 asset is processed per polling cycle regardless of catalog size.
- **SC-002**: Polling cycle duration with a filter configured is proportional to processing a single asset, not the full catalog size (no unnecessary negotiations or transfers).
- **SC-003**: Existing deployments without an asset filter configured experience no change in behavior after the feature is deployed.
- **SC-004**: Platform operators can confirm the active filter status within 10 seconds by checking adapter startup logs.
- **SC-005**: When the filtered asset is not found in the catalog, the operator is notified via a warning log entry within the same polling cycle.
