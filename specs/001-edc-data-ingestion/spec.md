# Feature Specification: EDC Data Ingestion

**Feature Branch**: `001-edc-data-ingestion`
**Created**: 2026-03-09
**Status**: Draft
**Input**: User description: "setze die erste implementierung um, um daten via edc in hono zu bringen."

## Clarifications

### Session 2026-03-09

- Q: What is the MVP scope boundary for this first implementation? → A: Multi-tenant, multiple devices — full scope as currently specified. No features deferred.
- Q: What is the asset-to-device naming convention? → A: Asset ID = Device ID directly. Device IDs are globally unique across all tenants, so no tenant prefix is needed.
- Q: Should the adapter cache/reuse contract agreements or negotiate fresh each cycle? → A: Negotiate fresh each polling cycle. Simplicity over performance for the first implementation.
- Q: Polling granularity — single global cycle or per-tenant? → A: Single global polling cycle covering all tenants and devices sequentially.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Fetch Telemetry from EDC Consumer (Priority: P1)

As a platform operator, I want the Hono EDC adapter to periodically fetch telemetry data from an external EDC Consumer connector and forward it into Hono's messaging infrastructure, so that device data available in a dataspace becomes accessible to all downstream Hono consumers.

**Why this priority**: This is the core value proposition — without telemetry flowing from EDC into Hono, the adapter has no purpose. Everything else depends on this working.

**Independent Test**: Can be fully tested by configuring the adapter with an EDC Consumer endpoint, triggering a fetch cycle, and verifying that telemetry messages appear on Hono's messaging infrastructure (AMQP/Kafka) with correct device metadata.

**Acceptance Scenarios**:

1. **Given** the adapter is configured with a valid EDC Consumer Management API endpoint and a registered device exists in Hono, **When** a polling cycle triggers, **Then** the adapter queries the EDC catalog, finds a matching asset for the device, and fetches the telemetry data.
2. **Given** telemetry data has been successfully fetched from the EDC data plane, **When** the adapter processes the response, **Then** it forwards the telemetry to Hono's messaging infrastructure with correct tenant ID, device ID, and content type metadata.
3. **Given** the adapter is running, **When** the configured polling interval elapses, **Then** the adapter automatically initiates a new fetch cycle without manual intervention.

---

### User Story 2 - EDC Contract Negotiation (Priority: P1)

As a platform operator, I want the adapter to handle EDC contract negotiation automatically before fetching data, so that all data transfers comply with dataspace policies and data sovereignty requirements.

**Why this priority**: Without a valid contract agreement, no data transfer can occur in the EDC ecosystem. This is a prerequisite for any data flow.

**Independent Test**: Can be tested by pointing the adapter at an EDC Consumer connector with a known catalog, verifying that the adapter initiates contract negotiation and only fetches data after a successful agreement.

**Acceptance Scenarios**:

1. **Given** the adapter discovers an asset in the EDC catalog matching a Hono device, **When** no existing contract agreement exists, **Then** the adapter initiates a contract negotiation via the EDC Management API.
2. **Given** a contract negotiation is in progress, **When** the negotiation succeeds, **Then** the adapter proceeds with data transfer initiation.
3. **Given** a contract negotiation is in progress, **When** the negotiation fails (policy rejection), **Then** the adapter logs the failure with correlation ID and skips the device without affecting other devices.

---

### User Story 3 - Device-to-Asset Mapping (Priority: P2)

As a platform operator, I want the adapter to map EDC dataspace assets to Hono device identities (tenant + device ID), so that fetched telemetry is correctly attributed to the right device.

**Why this priority**: Correct attribution of telemetry to devices is essential for data integrity, but the mapping mechanism can initially be simple (convention-based naming) and refined later.

**Independent Test**: Can be tested by registering devices in Hono and configuring corresponding assets in the EDC catalog, then verifying that the adapter correctly matches and attributes telemetry.

**Acceptance Scenarios**:

1. **Given** a device is registered in Hono with device ID "sensor-001", **When** the adapter queries the EDC catalog, **Then** it identifies the asset with ID "sensor-001" as the corresponding dataspace asset for that device.
2. **Given** a dataspace asset has no corresponding device registered in Hono, **When** the adapter encounters this asset during catalog query, **Then** it skips the asset and logs a warning.
3. **Given** multiple devices across multiple tenants are registered in Hono, **When** a polling cycle runs, **Then** each device's telemetry is fetched and forwarded with the correct tenant context.

---

### User Story 4 - Operational Health & Observability (Priority: P3)

As a platform operator, I want the adapter to expose health endpoints and basic metrics, so that I can monitor its operational status and detect issues early.

**Why this priority**: Observability is important for production readiness but is not required for the initial data flow to work.

**Independent Test**: Can be tested by starting the adapter and querying the health and metrics endpoints, verifying correct responses based on EDC Consumer connectivity status.

**Acceptance Scenarios**:

1. **Given** the adapter is running and the EDC Consumer is reachable, **When** the readiness endpoint is queried, **Then** it returns a healthy status.
2. **Given** the EDC Consumer connector is unreachable, **When** the readiness endpoint is queried, **Then** it returns an unhealthy status.
3. **Given** the adapter has completed fetch cycles, **When** the metrics endpoint is queried, **Then** it reports telemetry fetch counts, success/failure rates, and latency.

---

### Edge Cases

- What happens when the EDC Consumer connector becomes unavailable mid-transfer?
- What happens when a device is deregistered from Hono while a fetch is in progress?
- What happens when the EDC data plane returns malformed or empty telemetry payloads?
- What happens when the polling interval is shorter than the time to complete a full fetch cycle?
- What happens when a tenant is disabled in Hono but has active EDC contracts?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST poll the EDC Consumer Management API at a configurable interval using a single global polling cycle that covers all tenants and devices sequentially.
- **FR-002**: System MUST negotiate a fresh contract with the EDC Consumer connector before each data transfer. Contract agreements are not cached or reused across polling cycles.
- **FR-003**: System MUST only fetch telemetry for devices that are registered in Hono (resolved via Device Registration API).
- **FR-004**: System MUST forward fetched telemetry into Hono's messaging infrastructure (AMQP 1.0 or Kafka) with correct metadata (tenant ID, device ID, content type, TTL).
- **FR-005**: System MUST map dataspace assets to Hono device identities by matching the EDC asset ID directly to the Hono device ID. Device IDs are globally unique across all tenants, so no tenant prefix is required in the asset ID.
- **FR-006**: System MUST handle transient EDC failures with retry and exponential backoff.
- **FR-007**: System MUST respect tenant-level configuration (enabled/disabled status, rate limits) when fetching telemetry.
- **FR-008**: System MUST expose health check endpoints (liveness and readiness) reflecting EDC Consumer connectivity status.
- **FR-009**: System MUST log all EDC interactions (catalog queries, negotiations, transfers) with correlation IDs.
- **FR-010**: System MUST enforce TLS for all communication with the external EDC Consumer connector.
- **FR-011**: System MUST skip fetch cycles for assets where contract negotiation fails without affecting other devices.
- **FR-012**: System MUST validate telemetry data received from the EDC data plane before forwarding to Hono.

### Key Entities

- **EDC Catalog Asset**: Represents a telemetry data offering from an EDC Provider, discoverable via the Consumer's catalog API. Contains asset ID, policies, and data address information.
- **Contract Agreement**: The result of a successful negotiation between EDC Consumer and Provider, authorizing data transfer. Includes agreement ID, asset reference, and usage policy.
- **Data Transfer**: An active or completed transfer of telemetry data from the EDC data plane to the adapter. Includes transfer ID, state, and data endpoint.
- **Device Mapping**: The association between a dataspace asset and a Hono device identity. Asset ID maps directly to device ID (globally unique). The adapter resolves the tenant from Hono's Device Registration API.
- **Telemetry Message**: The normalized message forwarded into Hono's messaging infrastructure, carrying device metadata and the payload received from EDC.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Telemetry data from a dataspace asset is successfully delivered to Hono's messaging infrastructure within 30 seconds of a fetch cycle starting.
- **SC-002**: The adapter correctly attributes 100% of fetched telemetry to the right Hono device (tenant + device ID).
- **SC-003**: Failed contract negotiations do not block or delay telemetry fetching for other devices.
- **SC-004**: The adapter recovers from EDC Consumer unavailability within 3 polling cycles after connectivity is restored.
- **SC-005**: Platform operators can determine the adapter's health status and last successful fetch time from the exposed endpoints.
- **SC-006**: All EDC interactions are traceable end-to-end via correlation IDs in structured logs.
