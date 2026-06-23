# Implementation Plan: Single Asset Filter

**Branch**: `003-single-asset-filter` | **Date**: 2026-03-10 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/003-single-asset-filter/spec.md`

## Summary

Add an optional `assetIdFilter` configuration property to the EDC adapter that restricts polling to a single named asset from the EDC catalog. When configured, only the matching asset undergoes contract negotiation and data transfer, eliminating unnecessary processing. When absent, all assets are processed as before (backward compatible). The change touches two existing classes (`EdcAdapterProperties`, `TelemetryPollingService`) and their corresponding tests.

## Technical Context

**Language/Version**: Java 21+ (LTS) with records, sealed classes, pattern matching
**Primary Dependencies**: Quarkus 3.x, Vert.x, Hono adapter-base (existing — no new dependencies)
**Storage**: N/A (stateless adapter; configuration only)
**Testing**: JUnit 5, Mockito, WireMock (existing test infrastructure)
**Target Platform**: Linux server (container), Quarkus JVM mode
**Project Type**: Single module within Hono monorepo (`adapters/edc`)
**Performance Goals**: Polling cycle with filter processes 1 asset regardless of catalog size (SC-001, SC-002)
**Constraints**: No blocking on Vert.x event loop; backward compatible with existing deployments
**Scale/Scope**: Minimal change — 1 new config property, 1 new filter method, logging additions

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Hono Adapter Compliance | PASS | No changes to Hono adapter contract; filtering is internal to polling logic |
| II. Modern Java & Vert.x/Quarkus | PASS | Uses existing Java patterns (Optional, Stream API); no blocking operations |
| III. Test-First Development | PASS | Unit tests for filter logic, integration tests for config validation and cycle behavior |
| IV. Error Handling & Input Validation | PASS | Config validation at startup, warning logs for missing assets, correlation IDs preserved |
| V. Tooling & Static Analysis | PASS | No new dependencies; inherits existing Spotless/SpotBugs configuration |
| VI. Security & Data Sovereignty | PASS | No security surface change; filter is a local config property, not exposed externally |
| VII. Performance & Scheduled Polling | PASS | Filter reduces work per cycle; no new blocking or event loop concerns |
| VIII. EDC Consumer Integration | PASS | Catalog query unchanged; filter applied in-memory post-query |
| IX. Observability | PASS | Startup log, cycle log, and warning log for filter status |

**Gate result**: ALL PASS — proceed to implementation.

**Post-Phase 1 re-check**: ALL PASS — no design decisions introduced constitution violations.

## Project Structure

### Documentation (this feature)

```text
specs/003-single-asset-filter/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── no-new-contracts.md
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
    │   │   ├── EdcAdapterProperties.java               # MODIFY: add assetIdFilter field
    │   │   └── polling/
    │   │       └── TelemetryPollingService.java         # MODIFY: add filterAssets(), update executeCycle()
    │   └── resources/
    │       └── application.yaml                         # MODIFY: add assetIdFilter example (commented)
    └── test/
        └── java/org/eclipse/hono/adapter/edc/
            ├── EdcAdapterPropertiesTest.java            # MODIFY: add filter validation tests
            └── polling/
                └── TelemetryPollingServiceTest.java     # MODIFY: add filter behavior tests
```

**Structure Decision**: No new files or packages. Changes are confined to 2 existing production classes + their tests + configuration example. This is a targeted enhancement to the existing polling pipeline.

## Complexity Tracking

No constitution violations to justify. All design choices align with established patterns.
