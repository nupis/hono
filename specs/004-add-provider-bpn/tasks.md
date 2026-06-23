# Tasks: Add Provider BPN

**Input**: Design documents from `/specs/004-add-provider-bpn/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Included per constitution Principle III (Test-First Development).

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Production**: `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/`
- **Models**: `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/client/model/`
- **Tests**: `adapters/edc/src/test/java/org/eclipse/hono/adapter/edc/`
- **Config**: `adapters/edc/src/main/resources/application.yaml`

---

## Phase 1: Setup

**Purpose**: Verify build, review modification points.

- [X] T001 Verify existing build passes: run `mvn clean test` from `adapters/edc/`
- [X] T002 Review the three request model records (`CatalogRequest.java`, `ContractNegotiationRequest.java`, `TransferRequest.java`) in `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/client/model/` to confirm factory method signatures
- [X] T003 Review `EdcManagementClient.java` at `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/client/EdcManagementClient.java` to confirm `queryCatalog()`, `initiateNegotiation()`, and `initiateTransfer()` method signatures

**Checkpoint**: Build green, modification points understood.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Add `providerBpn` configuration property. All user stories depend on this.

**⚠️ CRITICAL**: US1, US2, and US3 all depend on `providerBpn` being available in config.

- [X] T004 Add `providerBpn` field (required String, non-null) with getter and setter to `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/EdcAdapterProperties.java` — setter must trim whitespace; throw `IllegalArgumentException` if blank
- [X] T005 Add `Optional<String> providerBpn()` method to `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/EdcAdapterOptions.java` and wire it in the `EdcAdapterProperties(EdcAdapterOptions)` constructor via `options.providerBpn().ifPresent(this::setProviderBpn)`
- [X] T006 Add unit tests for `providerBpn` property in `adapters/edc/src/test/java/org/eclipse/hono/adapter/edc/EdcAdapterPropertiesTest.java` — test: valid string stored, trimmed whitespace, blank throws IllegalArgumentException, null throws NullPointerException

**Checkpoint**: Configuration property exists, validated, and tested. User stories can begin.

---

## Phase 3: User Story 1 — Configure Provider BPN for EDC Interactions (Priority: P1) 🎯 MVP

**Goal**: All three EDC Management API requests include the provider BPN in the correct field (`counterPartyId` or `connectorId`).

**Independent Test**: Configure adapter with BPN, trigger a polling cycle, verify BPN appears in catalog, negotiation, and transfer request payloads.

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T007 [P] [US1] Unit test in `adapters/edc/src/test/java/org/eclipse/hono/adapter/edc/client/EdcManagementClientTest.java` — test that `queryCatalog(providerDspUrl, providerBpn)` sends a request body containing `"counterPartyId": "{providerBpn}"`
- [X] T008 [P] [US1] Unit test in `adapters/edc/src/test/java/org/eclipse/hono/adapter/edc/client/EdcManagementClientTest.java` — test that `initiateNegotiation(providerDspUrl, providerBpn, offerId, assetId)` sends a request body containing `"counterPartyId": "{providerBpn}"`
- [X] T009 [P] [US1] Unit test in `adapters/edc/src/test/java/org/eclipse/hono/adapter/edc/client/EdcManagementClientTest.java` — test that `initiateTransfer(providerDspUrl, providerBpn, contractAgreementId)` sends a request body containing `"connectorId": "{providerBpn}"` (not hardcoded "provider")

### Implementation for User Story 1

- [X] T010 [P] [US1] Add `counterPartyId` field to `CatalogRequest` record and update `create()` factory to accept `providerBpn` parameter in `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/client/model/CatalogRequest.java`
- [X] T011 [P] [US1] Add `counterPartyId` field to `ContractNegotiationRequest` record and update `create()` factory to accept `providerBpn` parameter in `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/client/model/ContractNegotiationRequest.java`
- [X] T012 [P] [US1] Update `TransferRequest.create()` factory to accept `providerBpn` parameter replacing hardcoded `"provider"` for `connectorId` in `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/client/model/TransferRequest.java`
- [X] T013 [US1] Update `EdcManagementClient.queryCatalog()` to accept `providerBpn` parameter and pass it to `CatalogRequest.create()` in `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/client/EdcManagementClient.java`
- [X] T014 [US1] Update `EdcManagementClient.initiateNegotiation()` to accept `providerBpn` parameter and pass it to `ContractNegotiationRequest.create()` in `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/client/EdcManagementClient.java`
- [X] T015 [US1] Update `EdcManagementClient.initiateTransfer()` to accept `providerBpn` parameter and pass it to `TransferRequest.create()` in `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/client/EdcManagementClient.java`
- [X] T016 [US1] Update `TelemetryPollingService` to pass `properties.getProviderBpn()` to all three management client method calls in `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/polling/TelemetryPollingService.java`
- [X] T017 [US1] Update all existing tests in `adapters/edc/src/test/java/org/eclipse/hono/adapter/edc/polling/TelemetryPollingServiceTest.java` to pass `providerBpn` to the mocked management client calls (fix compilation)
- [X] T018 [US1] Verify all tests pass: run `mvn test -pl adapters/edc`

**Checkpoint**: All EDC API requests include the provider BPN. MVP complete.

---

## Phase 4: User Story 2 — Validate Provider BPN at Startup (Priority: P1)

**Goal**: Adapter fails to start with a clear error when `providerBpn` is not configured.

**Independent Test**: Start adapter without BPN, verify startup failure with descriptive message.

### Tests for User Story 2

- [X] T019 [P] [US2] Unit test in `adapters/edc/src/test/java/org/eclipse/hono/adapter/edc/EdcProtocolAdapterTest.java` — test that `doStart()` fails with "provider BPN must be configured" when `providerBpn` is null
- [X] T020 [P] [US2] Unit test in `adapters/edc/src/test/java/org/eclipse/hono/adapter/edc/EdcProtocolAdapterTest.java` — test that `doStart()` succeeds when `providerBpn` is configured alongside `managementApiUrl` and `managementApiKey`

### Implementation for User Story 2

- [X] T021 [US2] Add `providerBpn` validation to `doStart()` in `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/EdcProtocolAdapter.java` — check `config.getProviderBpn() == null` alongside existing `managementApiUrl`/`managementApiKey` checks; fail with "EDC provider BPN must be configured"
- [X] T022 [US2] Verify tests pass: run `mvn test -pl adapters/edc -Dtest=EdcProtocolAdapterTest`

**Checkpoint**: Adapter rejects startup without BPN, provides clear error message.

---

## Phase 5: User Story 3 — Operational Traceability of Provider Identity (Priority: P2)

**Goal**: Provider BPN visible in startup and polling cycle logs.

**Independent Test**: Configure BPN, start adapter, verify BPN in logs.

### Implementation for User Story 3

- [X] T023 [US3] Add BPN to startup log in `doStart()` of `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/EdcProtocolAdapter.java` — log INFO "EDC protocol adapter starting [providerBpn={}]" with the configured BPN
- [X] T024 [US3] Add BPN to debug log messages in `queryCatalog()`, `initiateNegotiation()`, and `initiateTransfer()` of `adapters/edc/src/main/java/org/eclipse/hono/adapter/edc/client/EdcManagementClient.java` — include `providerBpn` in existing LOG.debug() calls
- [X] T025 [US3] Verify all tests still pass: run `mvn test -pl adapters/edc`

**Checkpoint**: Operators can see BPN in startup and API interaction logs.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Configuration documentation, final validation.

- [X] T026 [P] Add `providerBpn` as required property to `adapters/edc/src/main/resources/application.yaml` with environment variable reference `${HONO_EDC_PROVIDER_BPN}`
- [X] T027 [P] Run full test suite: `mvn clean verify -pl adapters/edc` to confirm no regressions
- [X] T028 Validate quickstart.md scenarios from `specs/004-add-provider-bpn/quickstart.md` — verify log messages and error messages match expected format

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup — adds config property that BLOCKS all stories
- **US1 (Phase 3)**: Depends on Foundational — BPN in API requests (MVP)
- **US2 (Phase 4)**: Depends on Foundational — startup validation (can run in parallel with US1)
- **US3 (Phase 5)**: Depends on US1 — logging uses BPN parameter already added in US1
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Depends on Phase 2 only. Core BPN propagation.
- **User Story 2 (P1)**: Depends on Phase 2 only. Independent startup validation.
- **User Story 3 (P2)**: Depends on US1 (the `providerBpn` parameter in client methods must exist for logging).

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Request models before client, client before polling service
- Story complete before moving to next priority

### Parallel Opportunities

- T007, T008, T009 (US1 tests) can all run in parallel
- T010, T011, T012 (US1 model changes) can all run in parallel
- T019, T020 (US2 tests) can run in parallel
- T026, T027 (Polish) can run in parallel
- US1 and US2 can start in parallel after Phase 2

---

## Parallel Example: User Story 1

```bash
# Launch all US1 tests together (write first, expect failures):
Task: "T007 - Unit test: catalog query includes counterPartyId"
Task: "T008 - Unit test: negotiation includes counterPartyId"
Task: "T009 - Unit test: transfer uses BPN instead of 'provider'"

# Launch all model changes in parallel:
Task: "T010 - Add counterPartyId to CatalogRequest"
Task: "T011 - Add counterPartyId to ContractNegotiationRequest"
Task: "T012 - Update TransferRequest.create() to accept BPN"

# Then sequentially update client and service:
Task: "T013-T015 - Update EdcManagementClient method signatures"
Task: "T016 - Update TelemetryPollingService to pass BPN"
Task: "T017 - Fix existing tests for new signatures"
Task: "T018 - Verify all tests pass"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (verify build)
2. Complete Phase 2: Foundational (add config property)
3. Complete Phase 3: User Story 1 (BPN in all API requests)
4. **STOP and VALIDATE**: Test with a real EDC Consumer
5. Deploy if ready — all API requests use correct BPN

### Incremental Delivery

1. Setup + Foundational → Config property ready
2. Add US1 → BPN in requests → Deploy (MVP!)
3. Add US2 → Startup validation → Deploy
4. Add US3 → Logging → Deploy
5. Polish → Config docs, final validation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story
- **Breaking change**: Adapter now requires `providerBpn` at startup
- Total: **28 tasks** across 6 phases
- 7 production files modified, 3 test files modified
- Commit after each completed phase
