# Tasks: EDC Data Ingestion

**Input**: Design documents from `/specs/001-edc-data-ingestion/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Included per Constitution Principle III (Test-First Development is NON-NEGOTIABLE).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization, dependencies, and basic directory structure

- [x] T001 Update Maven POM with required dependencies (Quarkus REST Client, WireMock, Hono adapter-base, test-utils) in adapters/edc/pom.xml
- [x] T002 Create source directory structure under adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/ with subdirectories: app/, polling/, client/, client/model/, mapping/
- [x] T003 Create test directory structure under adapters/edc/src/test/java/org/eclipse/hono/adapter/edc/ with subdirectories: polling/, client/, mapping/
- [x] T004 Create Quarkus application configuration with EDC adapter properties in adapters/edc/src/main/resources/application.yaml

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**CRITICAL**: No user story work can begin until this phase is complete

- [x] T005 Implement EdcAdapterProperties extending ProtocolAdapterProperties with all configuration fields (pollingInterval, managementApiUrl, managementApiKey, providerDspUrl, timeouts, retry settings) in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/EdcAdapterProperties.java
- [x] T006 [P] Create EDC API request/response model records: CatalogRequest in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/client/model/CatalogRequest.java
- [x] T007 [P] Create CatalogResponse record (DCAT catalog with dataset extraction) in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/client/model/CatalogResponse.java
- [x] T008 [P] Create ContractNegotiationRequest record in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/client/model/ContractNegotiationRequest.java
- [x] T009 [P] Create ContractNegotiationState record with NegotiationState enum (REQUESTED, AGREED, VERIFIED, FINALIZED, TERMINATED) in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/client/model/ContractNegotiationState.java
- [x] T010 [P] Create TransferRequest record in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/client/model/TransferRequest.java
- [x] T011 [P] Create TransferProcessState record with TransferState enum (REQUESTED, PROVISIONED, STARTED, COMPLETED, TERMINATED) in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/client/model/TransferProcessState.java
- [x] T012 [P] Create EndpointDataReference record (endpoint, authorization, contractId) in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/client/model/EndpointDataReference.java
- [x] T013 [P] Create CatalogAsset record (assetId, offerId, providerDspUrl) in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/client/model/CatalogAsset.java
- [x] T014 [P] Create DeviceTelemetryResult record (deviceId, tenantId, contentType, payload, correlationId) in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/client/model/DeviceTelemetryResult.java
- [x] T015 Implement EdcProtocolAdapter extending AbstractProtocolAdapterBase<EdcAdapterProperties> with getTypeName() returning "hono-edc", doStart/doStop lifecycle hooks in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/EdcProtocolAdapter.java
- [x] T016 Implement Quarkus Application class extending AbstractProtocolAdapterApplication<EdcAdapterProperties> with CDI wiring for adapter and service clients in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/app/Application.java

**Checkpoint**: Foundation ready - adapter compiles, starts as Quarkus app, connects to Hono services. User story implementation can now begin.

---

## Phase 3: User Story 2 - EDC Contract Negotiation (Priority: P1)

**Goal**: Build the EDC Management API client layer that handles catalog query, contract negotiation, transfer initiation, and EDR retrieval. This is the prerequisite for all data fetching.

**Independent Test**: Verify that the client correctly calls EDC Management API v3 endpoints, handles negotiation state transitions, and retrieves EDR — tested against WireMock.

### Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T017 [P] [US2] Write unit tests for EdcManagementClient: catalog query, negotiation initiation, state polling, transfer initiation, EDR retrieval — mock HTTP responses in adapters/edc/src/test/java/org/eclipse/hono/adapter/edc/client/EdcManagementClientTest.java
- [x] T018 [P] [US2] Write unit tests for EdcDataPlaneClient: HTTP GET with authorization header, payload extraction, error handling in adapters/edc/src/test/java/org/eclipse/hono/adapter/edc/client/EdcDataPlaneClientTest.java

### Implementation for User Story 2

- [x] T019 [US2] Implement EdcManagementClient using Quarkus REST Client with methods: queryCatalog(providerDspUrl), initiateNegotiation(offer), getNegotiationState(id), initiateTransfer(agreementId), getTransferState(id), getEndpointDataReference(transferId). Include X-Api-Key auth header, JSON-LD serialization, and correlation ID propagation in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/client/EdcManagementClient.java
- [x] T020 [US2] Implement EdcDataPlaneClient with method: pullTelemetry(endpointDataReference) that performs HTTP GET with Bearer authorization and returns raw payload. Include configurable timeout and error handling in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/client/EdcDataPlaneClient.java
- [x] T021 [US2] Add retry with exponential backoff to EdcManagementClient for transient failures (connection errors, 5xx responses) using configurable maxRetries and retryBackoffBase from EdcAdapterProperties in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/client/EdcManagementClient.java
- [x] T022 [US2] Add negotiation and transfer state polling loops with configurable poll interval and timeout to EdcManagementClient. Poll negotiation until FINALIZED/TERMINATED, poll transfer until STARTED/TERMINATED in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/client/EdcManagementClient.java

**Checkpoint**: EDC client layer complete. Can query catalog, negotiate contracts, initiate transfers, retrieve EDR, and pull telemetry data — all verified against WireMock mocks.

---

## Phase 4: User Story 1 - Fetch Telemetry from EDC Consumer (Priority: P1) MVP

**Goal**: Build the polling orchestrator that periodically fetches telemetry from EDC and forwards it into Hono's messaging infrastructure with correct device metadata.

**Independent Test**: Configure adapter with mocked EDC endpoint, trigger a polling cycle, verify telemetry appears on mocked TelemetrySender with correct tenant ID, device ID, and content type.

**Depends on**: US2 (EDC client layer)

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T023 [P] [US1] Write unit tests for TelemetryPollingService: single cycle execution, iterates catalog assets, calls negotiate → transfer → pull → forward for each device, skips failed negotiations without blocking others in adapters/edc/src/test/java/org/eclipse/hono/adapter/edc/polling/TelemetryPollingServiceTest.java
- [x] T024 [P] [US1] Write unit tests for EdcProtocolAdapter: verify doStart registers periodic timer, doStop cancels timer, polling interval from configuration in adapters/edc/src/test/java/org/eclipse/hono/adapter/edc/EdcProtocolAdapterTest.java

### Implementation for User Story 1

- [x] T025 [US1] Implement TelemetryPollingService with executeCycle() method that: (1) queries EDC catalog, (2) for each asset resolves device via DeviceRegistrationClient, (3) negotiates contract, (4) initiates transfer, (5) retrieves EDR, (6) pulls telemetry, (7) forwards via TelemetrySender.sendTelemetry() with correct tenant, device, QoS, contentType, payload, and correlation ID in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/polling/TelemetryPollingService.java
- [x] T026 [US1] Wire TelemetryPollingService into EdcProtocolAdapter lifecycle: start Vert.x setPeriodic timer in doStart(), cancel timer in doStop(), use pollingInterval from EdcAdapterProperties in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/EdcProtocolAdapter.java
- [x] T027 [US1] Add structured logging with correlation IDs to TelemetryPollingService: log cycle start/end, per-device negotiation/transfer/forward events, and errors with SLF4J in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/polling/TelemetryPollingService.java
- [x] T028 [US1] Add tenant-level checks to TelemetryPollingService: verify tenant is enabled via TenantClient.get(), check message limits via checkMessageLimit() before forwarding telemetry in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/polling/TelemetryPollingService.java
- [x] T029 [US1] Add telemetry payload validation in TelemetryPollingService: reject null/empty payloads, validate size limits, log and skip invalid data in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/polling/TelemetryPollingService.java
- [x] T030 [US1] Handle overlapping polling cycles: skip new cycle if previous is still running, log warning in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/polling/TelemetryPollingService.java

**Checkpoint**: Full end-to-end pipeline works: adapter starts, polls EDC on schedule, negotiates contracts, fetches telemetry, forwards to Hono. This is the MVP.

---

## Phase 5: User Story 3 - Device-to-Asset Mapping (Priority: P2)

**Goal**: Implement the device-to-asset mapping logic that resolves EDC asset IDs to Hono device identities (tenant + device ID) using direct ID matching.

**Independent Test**: Given registered devices in Hono, verify that the mapper correctly resolves asset IDs to (tenantId, deviceId) pairs and skips unregistered assets with appropriate warnings.

### Tests for User Story 3

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T031 [P] [US3] Write unit tests for DeviceAssetMapper: asset ID maps to device ID, resolves tenant from registration, skips unknown asset IDs, handles multi-tenant scenarios, logs warnings for unmatched assets in adapters/edc/src/test/java/org/eclipse/hono/adapter/edc/mapping/DeviceAssetMapperTest.java

### Implementation for User Story 3

- [x] T032 [US3] Implement DeviceAssetMapper with method: resolveDevice(assetId) → Future<Optional<DeviceMapping>> that queries DeviceRegistrationClient.assertRegistration() across known tenants, returns (tenantId, deviceId) on match, empty on no match in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/mapping/DeviceAssetMapper.java
- [x] T033 [US3] Integrate DeviceAssetMapper into TelemetryPollingService: replace direct device lookup with mapper, handle Optional.empty() by skipping asset with warning log in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/polling/TelemetryPollingService.java

**Checkpoint**: Device-to-asset mapping works correctly across multiple tenants. Unregistered assets are gracefully skipped.

---

## Phase 6: User Story 4 - Operational Health & Observability (Priority: P3)

**Goal**: Expose health check endpoints and metrics for monitoring adapter status, EDC connectivity, and telemetry fetch performance.

**Independent Test**: Start adapter, query /health/ready and /metrics endpoints, verify correct health status based on EDC connectivity and correct metric values after fetch cycles.

### Tests for User Story 4

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T034 [P] [US4] Write unit tests for EdcAdapterMetrics: verify metric counters for fetch cycles, negotiations, transfers, successes, failures, and latency histograms in adapters/edc/src/test/java/org/eclipse/hono/adapter/edc/EdcAdapterMetricsTest.java (if needed beyond Micrometer auto-wiring)

### Implementation for User Story 4

- [x] T035 [US4] Implement EdcAdapterMetrics with Micrometer counters and timers: edc.adapter.cycles.total, edc.adapter.negotiations.total (tagged success/failure), edc.adapter.transfers.total (tagged success/failure), edc.adapter.telemetry.forwarded, edc.adapter.fetch.latency in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/EdcAdapterMetrics.java
- [x] T036 [US4] Integrate EdcAdapterMetrics into TelemetryPollingService: record cycle count, per-device negotiation/transfer outcomes, forwarded message count, and cycle duration in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/polling/TelemetryPollingService.java
- [x] T037 [US4] Add Quarkus SmallRye Health readiness check that verifies EDC Consumer Management API connectivity (simple catalog request or ping) in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/EdcProtocolAdapter.java
- [x] T038 [US4] Add last successful fetch timestamp to health check response and metrics in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/EdcAdapterMetrics.java

**Checkpoint**: Health endpoints return correct status, metrics track all EDC interactions, operators can monitor adapter performance.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Edge case handling, documentation, and final validation

- [x] T039 [P] Handle edge case: EDC Consumer unavailable mid-transfer — abort current device transfer, log error, continue to next device in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/polling/TelemetryPollingService.java
- [x] T040 [P] Handle edge case: tenant disabled during polling cycle — check tenant status before each device's telemetry forward in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/polling/TelemetryPollingService.java
- [x] T041 [P] Enforce TLS for all EDC communication — validate managementApiUrl uses HTTPS in EdcAdapterProperties, configure Quarkus REST Client TLS settings in adapters/edc/src/main/resources/application.yaml
- [x] T042 Add Javadoc to all public classes and methods per Documentation Standards in adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/
- [x] T043 Validate quickstart.md end-to-end scenario against running adapter
- [x] T044 Run full Maven build with Spotless formatting and SpotBugs analysis — fix any warnings in adapters/edc/

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **US2 (Phase 3)**: Depends on Foundational — builds EDC client layer
- **US1 (Phase 4)**: Depends on US2 — builds polling orchestrator using EDC client
- **US3 (Phase 5)**: Depends on Foundational — can start in parallel with US2/US1 but integrates with polling service
- **US4 (Phase 6)**: Depends on Foundational — can start in parallel with US2/US1 but integrates with polling service
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

- **US2 (P1 - Contract Negotiation)**: Start after Phase 2. No story dependencies. BLOCKS US1.
- **US1 (P1 - Fetch Telemetry)**: Start after US2. Depends on US2 for EDC client layer.
- **US3 (P2 - Device Mapping)**: Start after Phase 2. Can develop mapper independently, integration requires US1.
- **US4 (P3 - Observability)**: Start after Phase 2. Can develop metrics independently, integration requires US1.

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Model records before service logic
- Service logic before adapter integration
- Core implementation before error handling / edge cases
- Story complete before moving to next priority

### Parallel Opportunities

- T006–T014: All model records can be created in parallel (different files)
- T015, T016: Can run in parallel (different files)
- T017, T018: EDC client tests can run in parallel
- T023, T024: US1 tests can run in parallel
- T031: Can start in parallel with US2 implementation (different files)
- T034, T035: Can start in parallel with US1/US2 (different files)
- T039, T040, T041: Polish tasks on different concerns can run in parallel

---

## Parallel Example: Phase 2 (Foundational)

```bash
# Launch all model records in parallel (T006–T014):
Task: "Create CatalogRequest record in client/model/CatalogRequest.java"
Task: "Create CatalogResponse record in client/model/CatalogResponse.java"
Task: "Create ContractNegotiationRequest record in client/model/ContractNegotiationRequest.java"
Task: "Create ContractNegotiationState record in client/model/ContractNegotiationState.java"
Task: "Create TransferRequest record in client/model/TransferRequest.java"
Task: "Create TransferProcessState record in client/model/TransferProcessState.java"
Task: "Create EndpointDataReference record in client/model/EndpointDataReference.java"
Task: "Create CatalogAsset record in client/model/CatalogAsset.java"
Task: "Create DeviceTelemetryResult record in client/model/DeviceTelemetryResult.java"

# Then in parallel:
Task: "Implement EdcProtocolAdapter in EdcProtocolAdapter.java"
Task: "Implement Application class in app/Application.java"
```

## Parallel Example: User Story 2

```bash
# Launch tests in parallel:
Task: "Write EdcManagementClient tests in client/EdcManagementClientTest.java"
Task: "Write EdcDataPlaneClient tests in client/EdcDataPlaneClientTest.java"

# Then sequential implementation:
Task: "Implement EdcManagementClient"
Task: "Implement EdcDataPlaneClient"
Task: "Add retry with backoff"
Task: "Add state polling loops"
```

---

## Implementation Strategy

### MVP First (User Stories 1 + 2)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: US2 — EDC Contract Negotiation client
4. Complete Phase 4: US1 — Polling orchestrator + Hono forwarding
5. **STOP and VALIDATE**: Trigger polling cycle, verify telemetry arrives in Hono
6. Deploy/demo if ready — this is the core value proposition

### Incremental Delivery

1. Setup + Foundational → Adapter compiles and starts
2. Add US2 → EDC client layer works against mocked/real EDC
3. Add US1 → Full pipeline works (MVP!)
4. Add US3 → Device mapping is robust across tenants
5. Add US4 → Operators can monitor adapter health and performance
6. Polish → Edge cases, docs, build validation

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: US2 (EDC client) → US1 (polling orchestrator)
   - Developer B: US3 (device mapper, independent until integration)
   - Developer C: US4 (metrics + health, independent until integration)
3. Integration after US1 is complete

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Constitution Principle III requires test-first — all test tasks must precede implementation
- All EDC API interactions use JSON-LD format — see contracts/edc-management-api-usage.md for request/response examples
