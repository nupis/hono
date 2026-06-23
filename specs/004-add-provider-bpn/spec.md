# Feature Specification: Add Provider BPN

**Feature Branch**: `004-add-provider-bpn`
**Created**: 2026-03-10
**Status**: Draft
**Input**: User description: "Es werden für mehrere Aufrufe die BPN des Provider benötigt. Füge sie an den entsprechenden Stellen hinzu."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Configure Provider BPN for EDC Interactions (Priority: P1)

As a platform operator, I want to configure the Business Partner Number (BPN) of the EDC Provider so that all EDC Management API calls that require the provider's identity use the correct BPN instead of a placeholder value.

**Why this priority**: The BPN is a mandatory identifier in the Dataspace Protocol. Without the correct provider BPN, catalog queries may not properly identify the counter-party, and transfer requests use a hardcoded placeholder ("provider") instead of the actual participant identity. This can cause failures in production EDC environments that enforce participant validation.

**Independent Test**: Can be tested by configuring the adapter with a provider BPN, triggering a polling cycle, and verifying that the BPN appears in the catalog request, contract negotiation, and transfer request payloads sent to the EDC Consumer Management API.

**Acceptance Scenarios**:

1. **Given** the adapter is configured with a provider BPN (e.g., "BPNL00000003CRHK"), **When** a catalog query is sent to the EDC Consumer, **Then** the request includes the provider BPN as the counter-party identifier.
2. **Given** the adapter is configured with a provider BPN, **When** a transfer request is initiated, **Then** the request includes the provider BPN as the connector identifier (replacing the hardcoded placeholder).
3. **Given** the adapter is configured with a provider BPN, **When** a contract negotiation is initiated, **Then** the request includes the provider BPN as the counter-party identifier where required by the protocol.

---

### User Story 2 - Validate Provider BPN at Startup (Priority: P1)

As a platform operator, I want the adapter to validate that a provider BPN is configured at startup, so that I am immediately alerted if the configuration is missing rather than encountering failures during polling cycles.

**Why this priority**: A missing BPN will cause every EDC API call to fail or use incorrect identifiers. Early validation prevents wasted cycles and confusing runtime errors.

**Independent Test**: Can be tested by starting the adapter without a provider BPN configured and verifying that it fails with a clear error message.

**Acceptance Scenarios**:

1. **Given** the adapter is started without a provider BPN configured, **When** the adapter performs startup validation, **Then** it fails with a clear error message indicating the BPN is required.
2. **Given** the adapter is started with a valid provider BPN, **When** the adapter starts, **Then** it logs the configured BPN and proceeds normally.

---

### User Story 3 - Operational Traceability of Provider Identity (Priority: P2)

As a platform operator, I want to see the configured provider BPN in the adapter's logs, so that I can verify which provider identity is being used and troubleshoot identity-related issues.

**Why this priority**: Operational visibility helps diagnose issues where the wrong BPN is configured, but the core functionality does not depend on this.

**Independent Test**: Can be tested by configuring a provider BPN, starting the adapter, and verifying that the startup log and polling cycle logs include the BPN.

**Acceptance Scenarios**:

1. **Given** the adapter is configured with a provider BPN, **When** the adapter starts, **Then** the startup log includes the configured BPN.
2. **Given** the adapter is processing a polling cycle, **When** EDC API interactions are logged, **Then** the provider BPN is included in log entries for traceability.

---

### Edge Cases

- What happens when the configured BPN contains leading/trailing whitespace?
- What happens when the BPN format is invalid (e.g., too short, wrong prefix)?
- What happens when the EDC Consumer rejects the BPN as unrecognized?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST support a required configuration property for the EDC Provider's Business Partner Number (BPN).
- **FR-002**: System MUST include the provider BPN in catalog query requests as the counter-party identifier.
- **FR-003**: System MUST include the provider BPN in transfer initiation requests as the connector identifier (replacing the current hardcoded placeholder).
- **FR-004**: System MUST include the provider BPN in contract negotiation requests where the protocol requires a counter-party identifier.
- **FR-005**: System MUST validate that the provider BPN is configured at startup and fail with a clear error if it is missing or blank.
- **FR-006**: System MUST trim whitespace from the configured BPN value.
- **FR-007**: System MUST log the configured provider BPN at adapter startup.
- **FR-008**: System MUST include the provider BPN in structured log entries for EDC API interactions to support operational traceability.

### Key Entities

- **Provider BPN**: The Business Partner Number (BPN) identifying the EDC Provider in the dataspace. A string identifier (typically in the format "BPNL" followed by alphanumeric characters) required for all EDC protocol interactions that reference the provider.

## Assumptions

- The provider BPN is a static configuration value — a single adapter instance communicates with a single provider whose BPN does not change at runtime.
- The BPN format follows the Catena-X convention (e.g., "BPNL00000003CRHK") but the adapter does not enforce format validation beyond non-blank, as BPN formats may vary across dataspaces.
- The same BPN value is used across all three API call types (catalog, negotiation, transfer) for the same provider.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of EDC Management API requests sent by the adapter include the configured provider BPN in the appropriate field.
- **SC-002**: The adapter fails to start within 5 seconds with a descriptive error when no provider BPN is configured.
- **SC-003**: Platform operators can identify the configured provider BPN from the adapter's startup log within 10 seconds.
- **SC-004**: No EDC API request uses a hardcoded or placeholder provider identifier after this feature is deployed.
