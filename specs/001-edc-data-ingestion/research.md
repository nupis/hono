# Research: EDC Data Ingestion

**Branch**: `001-edc-data-ingestion` | **Date**: 2026-03-09

## R1: Hono Adapter Extension Pattern

**Decision**: Extend `AbstractProtocolAdapterBase<EdcAdapterProperties>` with a Quarkus `@ApplicationScoped` Application class.

**Rationale**: All existing Hono protocol adapters (AMQP, HTTP, MQTT, CoAP) follow this exact pattern. The base class provides lifecycle management, service client wiring (Tenant, Registration, Credentials, CommandRouter), messaging client providers (TelemetrySender, EventSender), and resource limit checks. Deviating would break Hono adapter compliance (Constitution Principle I).

**Alternatives considered**:
- Standalone Quarkus app without Hono base → Rejected: would not participate in Hono's tenant/device lifecycle, messaging infrastructure, or metrics. Would require reimplementing all service client integrations.
- Vert.x Verticle without Quarkus → Rejected: Hono's adapter ecosystem is standardized on Quarkus CDI for dependency injection and configuration.

**Key classes to extend/use**:
- `AbstractProtocolAdapterBase<T>` — main adapter logic
- `AbstractProtocolAdapterApplication<T>` — Quarkus CDI application
- `ProtocolAdapterProperties` — base configuration class
- `TelemetrySender.sendTelemetry(tenant, device, qos, contentType, payload, properties, context)`
- `DeviceRegistrationClient.assertRegistration(tenantId, deviceId, null, context)`
- `TenantClient.get(tenantId, context)`

## R2: EDC Management API Integration

**Decision**: Use Quarkus REST Client to call EDC Management API v3 directly via REST. No official Java client library exists.

**Rationale**: EDC does not provide an official standalone Java client library for external applications. The Maven artifacts (`management-api`, `management-api-lib`) are server-side connector extensions, not client SDKs. Quarkus REST Client is the natural choice in a Quarkus runtime and supports non-blocking I/O via Vert.x.

**Alternatives considered**:
- OpenAPI Generator client → Rejected: adds code generation complexity, generated code may not align with Quarkus/Vert.x patterns, and the JSON-LD format requires custom handling anyway.
- Raw `java.net.HttpClient` → Rejected: does not integrate with Quarkus CDI, configuration, or Vert.x event loop.
- Vert.x WebClient directly → Viable alternative, but Quarkus REST Client provides declarative interface, automatic configuration, and better testability.

**EDC Management API v3 endpoints used**:
| Operation | Method | Path |
|-----------|--------|------|
| Query catalog | POST | `/management/v3/catalog/request` |
| Start negotiation | POST | `/management/v3/contractnegotiations` |
| Get negotiation state | GET | `/management/v3/contractnegotiations/{id}` |
| Start transfer | POST | `/management/v3/transferprocesses` |
| Get transfer state | GET | `/management/v3/transferprocesses/{id}` |
| Get EDR (data address) | GET | `/management/v3/edrs/{transferProcessId}/dataaddress` |

**Authentication**: `X-Api-Key` header (configurable).

## R3: Data Transfer Pattern

**Decision**: Use `HttpData-PULL` transfer pattern. Consumer pulls data from the provider's data plane using an Endpoint Data Reference (EDR).

**Rationale**: HttpData-PULL is the recommended approach for on-demand data access in EDC. The flow is: (1) initiate transfer, (2) wait for STARTED state, (3) retrieve EDR from cache, (4) use EDR's endpoint URL and authorization token to HTTP GET the telemetry data. This is simpler than PUSH (no inbound HTTP server needed) and aligns with the adapter's role as an active consumer.

**Alternatives considered**:
- HttpData-PUSH → Rejected: requires the adapter to expose an HTTP endpoint for the provider to push data to. Adds complexity (inbound server, port management, NAT traversal) and conflicts with the polling model.
- Kafka-based streaming → Rejected: introduces Kafka dependency on the EDC side; not universally available at EDC Providers.

**Transfer flow**:
1. POST `/management/v3/transferprocesses` with `transferType: "HttpData-PULL"` and `contractAgreementId`
2. Poll GET `/management/v3/transferprocesses/{id}` until state = `STARTED`
3. GET `/management/v3/edrs/{transferProcessId}/dataaddress` → returns `{ endpoint, authorization }`
4. HTTP GET `{endpoint}` with `Authorization: {authorization}` header → telemetry payload
5. Forward payload to Hono via `TelemetrySender`

## R4: Polling and Scheduling

**Decision**: Use Vert.x `setPeriodic` timer for global polling cycle. Single cycle iterates over all tenants and their registered devices sequentially.

**Rationale**: Vert.x timers are non-blocking and integrate naturally with the event loop. A single global cycle is the simplest model per the clarification decision. Sequential device processing within a cycle avoids overwhelming the EDC Consumer connector.

**Alternatives considered**:
- Quarkus `@Scheduled` → Viable but less control over Vert.x context propagation and async composition.
- Per-tenant independent timers → Rejected per clarification: single global cycle decided.

**Configuration**:
- `hono.edc.pollingInterval` — interval between cycles (default: 60s)
- `hono.edc.managementApi.url` — EDC Consumer Management API base URL
- `hono.edc.managementApi.apiKey` — API key for authentication
- `hono.edc.dataPlane.requestTimeout` — timeout for data plane HTTP pull (default: 30s)
- `hono.edc.providerUrl` — EDC Provider DSP endpoint URL (counterPartyAddress)

## R5: Device-to-Asset Mapping

**Decision**: Asset ID = Device ID (direct 1:1 mapping). Tenant resolved from Hono's Device Registration API after matching.

**Rationale**: Per clarification, device IDs are globally unique across all tenants. The adapter queries the EDC catalog, extracts asset IDs, and for each asset ID attempts `DeviceRegistrationClient.assertRegistration()` to resolve the device and its tenant. No encoding/parsing logic needed.

**Alternatives considered**:
- Tenant-prefixed asset IDs (e.g., `tenant:deviceId`) → Rejected per clarification: not needed due to global uniqueness.
- Separate mapping configuration/database → Rejected: over-engineering for a direct ID match.

**Lookup flow**:
1. Query EDC catalog → list of assets with IDs
2. For each asset ID, treat it as a device ID
3. Look up all tenants (via Hono Tenant API) and attempt device registration assertion
4. If device found → proceed with contract negotiation and data transfer
5. If no device found → skip asset, log warning

## R6: JSON-LD Handling

**Decision**: Use Jackson with custom serialization for EDC JSON-LD request/response bodies. Model as Java records with `@JsonProperty` annotations for `@context`, `@type`, `@id` fields.

**Rationale**: EDC Management API v3 uses JSON-LD format. Java records with Jackson annotations provide immutable, type-safe models. The JSON-LD context is static (always `https://w3id.org/edc/v0.0.1/ns/`) so no dynamic context resolution is needed.

**Alternatives considered**:
- Full JSON-LD library (Titanium, jsonld-java) → Rejected: over-engineering for static contexts with known schemas.
- Raw `JsonObject` / Map-based approach → Rejected: loses type safety and makes testing harder.
