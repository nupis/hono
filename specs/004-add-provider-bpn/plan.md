# Implementation Plan: Add Provider BPN

**Branch**: `004-add-provider-bpn` | **Date**: 2026-03-10 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/004-add-provider-bpn/spec.md`

## Summary

Add a required `providerBpn` configuration property to the EDC adapter and propagate the provider's Business Partner Number (BPN) into all EDC Management API requests that reference the provider identity. This replaces the hardcoded `"provider"` placeholder in transfer requests and adds the missing `counterPartyId` field to catalog and negotiation requests. The change touches the configuration layer, the management client, three request model records, the polling service, and the protocol adapter startup.

## Technical Context

**Language/Version**: Java 21+ (LTS) with records, sealed classes, pattern matching
**Primary Dependencies**: Quarkus 3.x, Vert.x, Hono adapter-base (existing — no new dependencies)
**Storage**: N/A (stateless adapter; configuration only)
**Testing**: JUnit 5, Mockito, WireMock (existing test infrastructure)
**Target Platform**: Linux server (container), Quarkus JVM mode
**Project Type**: Single module within Hono monorepo (`adapters/edc`)
**Performance Goals**: No performance impact — BPN is a string parameter added to existing requests
**Constraints**: Breaking change — adapter now requires `providerBpn` at startup
**Scale/Scope**: Moderate change — 1 new config property, 3 request models updated, client method signatures updated

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Hono Adapter Compliance | PASS | No changes to Hono adapter contract; BPN is internal to EDC communication |
| II. Modern Java & Vert.x/Quarkus | PASS | Uses existing patterns (records, factory methods); no blocking operations |
| III. Test-First Development | PASS | Unit tests for each request model, integration tests for client methods, config validation tests |
| IV. Error Handling & Input Validation | PASS | Startup validation for required BPN; fail-fast on missing config |
| V. Tooling & Static Analysis | PASS | No new dependencies; inherits existing Spotless/SpotBugs configuration |
| VI. Security & Data Sovereignty | PASS | BPN enables proper participant identification — improves data sovereignty compliance |
| VII. Performance & Scheduled Polling | PASS | No impact — BPN is a string added to existing request payloads |
| VIII. EDC Consumer Integration | PASS | Directly improves EDC protocol compliance by including required participant identity |
| IX. Observability | PASS | BPN logged at startup and in EDC API debug logs |

**Gate result**: ALL PASS — proceed to implementation.

**Post-Phase 1 re-check**: ALL PASS — no design decisions introduced constitution violations.

## Project Structure

### Documentation (this feature)

```text
specs/004-add-provider-bpn/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── edc-management-api-bpn-changes.md
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
adapters/edc/
└── src/
    ├── main/
    │   ├── java/org/eclipse/hono/adapter/edc/
    │   │   ├── EdcAdapterOptions.java                  # MODIFY: add providerBpn() method
    │   │   ├── EdcAdapterProperties.java               # MODIFY: add providerBpn field, getter, setter
    │   │   ├── EdcProtocolAdapter.java                 # MODIFY: add providerBpn startup validation
    │   │   ├── client/
    │   │   │   ├── EdcManagementClient.java            # MODIFY: add providerBpn param to 3 methods
    │   │   │   └── model/
    │   │   │       ├── CatalogRequest.java             # MODIFY: add counterPartyId field
    │   │   │       ├── ContractNegotiationRequest.java # MODIFY: add counterPartyId field
    │   │   │       └── TransferRequest.java            # MODIFY: replace hardcoded connectorId
    │   │   └── polling/
    │   │       └── TelemetryPollingService.java        # MODIFY: pass providerBpn to client methods
    │   └── resources/
    │       └── application.yaml                        # MODIFY: add providerBpn property
    └── test/
        └── java/org/eclipse/hono/adapter/edc/
            ├── EdcAdapterPropertiesTest.java           # MODIFY: add providerBpn tests
            ├── client/
            │   └── EdcManagementClientTest.java        # MODIFY: update method signatures in tests
            └── polling/
                └── TelemetryPollingServiceTest.java    # MODIFY: pass providerBpn in tests
```

**Structure Decision**: No new files or packages. Changes are spread across existing production classes (7 files) + their tests (3 files) + configuration. This is a cross-cutting parameter addition.

## Complexity Tracking

No constitution violations to justify. All design choices align with established patterns.
