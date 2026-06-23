# Implementation Plan: EDC Data Ingestion

**Branch**: `001-edc-data-ingestion` | **Date**: 2026-03-09 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-edc-data-ingestion/spec.md`

## Summary

Build the Hono EDC adapter that periodically polls an external EDC Consumer connector's Management API (v3), negotiates contracts per device, fetches telemetry via the HttpData-PULL transfer pattern, and forwards it into Hono's messaging infrastructure (AMQP 1.0 / Kafka) as standard telemetry messages. The adapter extends Hono's `AbstractProtocolAdapterBase`, integrates with Hono's Device Registration, Tenant, and Credentials APIs, and runs as a Quarkus application within the Hono monorepo.

## Technical Context

**Language/Version**: Java 21+ (LTS) with records, sealed classes, pattern matching
**Primary Dependencies**: Quarkus 3.x, Vert.x, Hono adapter-base, Quarkus REST Client (for EDC Management API), Jackson (JSON-LD processing)
**Storage**: N/A (stateless adapter; no local persistence)
**Testing**: JUnit 5, Mockito, Hono test-utils (`ProtocolAdapterTestSupport`, `ProtocolAdapterMockSupport`), WireMock (for EDC API mocking)
**Target Platform**: Linux server (container), Quarkus JVM mode (native image feasible)
**Project Type**: Single module within Hono monorepo (`adapters/edc`)
**Performance Goals**: Telemetry delivery within 30 seconds of fetch cycle start (SC-001); non-blocking event loop
**Constraints**: No blocking on Vert.x event loop; TLS required for EDC communication; single global polling cycle
**Scale/Scope**: Multi-tenant, multiple devices per tenant; sequential processing within each polling cycle

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Hono Adapter Compliance | PASS | Extends `AbstractProtocolAdapterBase`, uses `TelemetrySender`, `DeviceRegistrationClient`, `TenantClient` |
| II. Modern Java & Vert.x/Quarkus | PASS | Java 21+, Quarkus runtime, Vert.x async patterns, Maven multi-module build |
| III. Test-First Development | PASS | Unit tests for domain logic, integration tests with mocked EDC API, Hono test utilities |
| IV. Error Handling & Input Validation | PASS | Structured errors with correlation IDs, EDC data validation at adapter boundary, retry with backoff |
| V. Tooling & Static Analysis | PASS | Inherits Spotless/SpotBugs from parent POM, SLF4J structured logging |
| VI. Security & Data Sovereignty | PASS | TLS enforced, no secrets in code, EDC policy enforcement, credentials via Hono APIs |
| VII. Performance & Scheduled Polling | PASS | Vert.x timer-based scheduling, non-blocking HTTP client, configurable polling interval |
| VIII. EDC Consumer Integration | PASS | Management API v3 REST calls, catalog query, contract negotiation, HttpData-PULL transfer, EDR-based data fetch |
| IX. Observability | PASS | Quarkus SmallRye Health, Micrometer metrics, OpenTelemetry tracing, correlation IDs |

**Gate result**: ALL PASS — proceed to Phase 0.

## Project Structure

### Documentation (this feature)

```text
specs/001-edc-data-ingestion/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── edc-management-api-usage.md
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
adapters/edc/
├── pom.xml                          # Already exists; extend with new dependencies
└── src/
    ├── main/
    │   ├── java/org/eclipse/hono/adapter/edc/
    │   │   ├── app/
    │   │   │   └── Application.java                    # Quarkus CDI entry point
    │   │   ├── EdcProtocolAdapter.java                 # Main adapter (extends AbstractProtocolAdapterBase)
    │   │   ├── EdcAdapterProperties.java               # Adapter configuration (extends ProtocolAdapterProperties)
    │   │   ├── EdcAdapterMetrics.java                  # Micrometer metrics
    │   │   ├── polling/
    │   │   │   └── TelemetryPollingService.java        # Scheduled polling orchestrator
    │   │   ├── client/
    │   │   │   ├── EdcManagementClient.java            # EDC Management API REST client
    │   │   │   ├── EdcDataPlaneClient.java             # EDC Data Plane HTTP pull client
    │   │   │   └── model/                              # EDC API request/response records
    │   │   │       ├── CatalogRequest.java
    │   │   │       ├── CatalogResponse.java
    │   │   │       ├── ContractNegotiationRequest.java
    │   │   │       ├── ContractNegotiationState.java
    │   │   │       ├── TransferRequest.java
    │   │   │       ├── TransferProcessState.java
    │   │   │       └── EndpointDataReference.java
    │   │   └── mapping/
    │   │       └── DeviceAssetMapper.java              # Asset ID ↔ Device ID mapping
    │   └── resources/
    │       └── application.yaml                        # Quarkus configuration
    └── test/
        └── java/org/eclipse/hono/adapter/edc/
            ├── EdcProtocolAdapterTest.java             # Unit tests
            ├── polling/
            │   └── TelemetryPollingServiceTest.java
            ├── client/
            │   ├── EdcManagementClientTest.java        # Integration tests with WireMock
            │   └── EdcDataPlaneClientTest.java
            └── mapping/
                └── DeviceAssetMapperTest.java
```

**Structure Decision**: Single module at `adapters/edc/` following the established Hono adapter pattern. The adapter extends `AbstractProtocolAdapterBase<EdcAdapterProperties>` and is wired via Quarkus CDI in `Application.java`, consistent with the AMQP/HTTP/MQTT adapters.

## Complexity Tracking

No constitution violations to justify. All design choices align with established patterns.
