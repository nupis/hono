<!--
Sync Impact Report
===================
Version change: 2.0.0 → 3.0.0 (MAJOR - complete redefinition of EDC
  integration model: adapter is now explicitly an EDC Consumer client
  connecting to externally hosted EDC infrastructure)
Modified principles:
  - IX. EDC Integration & Dataspace Compliance →
    IX. EDC Consumer Integration & Telemetry Fetching
    (scoped to consumer-side; provider is external)
  - I. Eclipse Hono Adapter Compliance →
    I. Eclipse Hono Adapter Compliance (refined: adapter
    fetches telemetry via EDC and injects into Hono pipeline)
  - VIII. Performance & Event Loop Discipline →
    VIII. Performance & Scheduled Polling Discipline
    (added polling/scheduling concerns for periodic fetching)
Added sections:
  - Principle IX rewritten to focus on EDC Consumer client role
  - Architectural context section under Core Principles preamble
Removed sections:
  - Push/pull data plane references (adapter is consumer-only)
Templates requiring updates:
  - .specify/templates/plan-template.md: ⚠ pending
    (Constitution Check gates need EDC Consumer specifics)
  - .specify/templates/spec-template.md: ✅ compatible
  - .specify/templates/tasks-template.md: ✅ compatible
Follow-up TODOs: none
-->

# Hono EDC Adapter Constitution

## Architectural Context

The EDC adapter resides at `./adapters/edc` within the Eclipse Hono
monorepo. Its purpose is to **consume telemetry data** from an
externally hosted Eclipse Dataspace Connector (EDC) infrastructure
on behalf of all devices registered in Hono.

**Data flow**:
1. External EDC Providers offer device telemetry as dataspace assets.
2. An external EDC Consumer connector negotiates contracts and
   provides a data plane endpoint.
3. This adapter calls the EDC Consumer's Management API and Data
   Plane API to fetch telemetry for each Hono-registered device.
4. Fetched telemetry is forwarded into Hono's standard messaging
   infrastructure (AMQP 1.0 / Kafka) as if it originated from a
   protocol adapter, enabling downstream processing (routing,
   command & control, event persistence).

The adapter does **not** host an EDC connector itself. It acts as a
**client of the external EDC Consumer connector**.

## Core Principles

### I. Eclipse Hono Adapter Compliance (NON-NEGOTIABLE)

The adapter MUST conform to Eclipse Hono's protocol adapter contract
so that telemetry fetched from the dataspace is indistinguishable
from telemetry received via MQTT, HTTP, or AMQP adapters.

- The adapter MUST implement Hono's standard adapter interfaces
  (telemetry, event messaging patterns).
- Device identity MUST be resolved via Hono's Device Registration
  API; only devices registered in Hono are eligible for dataspace
  telemetry fetching.
- Device authentication context (tenant, device ID) MUST be
  established using Hono's Credentials and Registration APIs.
- Telemetry messages forwarded to Hono's messaging infrastructure
  MUST carry correct metadata (tenant ID, device ID, content type,
  TTL) as defined by Hono's API.
- The adapter MUST participate in Hono's tenant-level configuration
  (rate limits, message TTL, enabled/disabled status).
- All Hono service client interactions (Tenant, Registration,
  Credentials, Command Router) MUST follow Hono's documented
  contracts.

---

### II. Modern Java & Vert.x/Quarkus Usage

The codebase MUST leverage current Java, Vert.x, and Quarkus
capabilities consistent with the Hono monorepo conventions.

- Java 21+ (LTS) with modern language features (records, sealed
  classes, pattern matching) is required.
- Vert.x async/reactive patterns MUST be used consistently;
  no blocking calls on the event loop.
- Quarkus is the target runtime; use Quarkus extensions and CDI
  for dependency injection.
- Prefer immutable data structures (Java records) unless mutation
  is explicitly justified.
- Maven is the build system; the adapter MUST integrate into
  Hono's multi-module build (parent POM: `adapters/parent`).
- Native image compilation MUST remain feasible; avoid reflection
  where possible or register it explicitly.

---

### III. Test-First Development (NON-NEGOTIABLE)

Tests define behavior and protect design.

- Tests MUST be written before or alongside implementation.
- Hono adapter protocol compliance requires integration tests
  against Hono's test utilities.
- EDC Consumer client interactions (catalog, negotiation, transfer)
  require integration tests with mocked EDC Management API
  responses.
- Domain logic requires unit tests with deterministic, isolated,
  fast execution.
- Bugs require a failing test before a fix is accepted.

---

### IV. Error Handling & Input Validation

Errors MUST be explicit, structured, and observable.

- Validate all data received from the EDC data plane at the adapter
  boundary before forwarding to Hono.
- Use Hono's standard error codes for device-facing error
  scenarios.
- EDC negotiation and transfer failures MUST be logged with
  correlation IDs and propagated as structured errors.
- Never swallow exceptions; always log with sufficient context.
- Vert.x Future/Promise rejections MUST always be handled.
- Transient EDC failures (network, timeouts) MUST trigger retry
  with exponential backoff and configurable limits.

---

### V. Tooling, Formatting & Static Analysis (MANDATORY)

Automation and consistency are non-optional.

- Code formatting MUST be enforced via a consistent formatter
  (Spotless with the project's configured style).
- Static analysis (SpotBugs, Error Prone, or equivalent) MUST be
  enabled with zero new warnings.
- Structured logging (SLF4J + JSON layout) is mandatory; no
  secrets or PII in logs.
- Dependencies MUST be minimal, justified, and regularly updated.
- All builds MUST pass linting, compilation, and tests before
  merge.

---

### VI. Security & Data Sovereignty (NON-NEGOTIABLE)

Security and data sovereignty are baseline requirements.

- Secrets MUST never be committed to source control.
- Device credentials MUST be validated via Hono's Credentials API;
  no custom credential stores.
- EDC policy enforcement MUST be respected; telemetry data MUST
  NOT be fetched or forwarded without a valid contract agreement.
- All data exchange with the external EDC Consumer MUST comply
  with dataspace policies (usage policies, access control,
  purpose limitation).
- TLS MUST be enforced for all communication with external EDC
  endpoints.
- Apply least-privilege principles for all credentials and
  service accounts (especially EDC Management API credentials).

---

### VII. Performance & Scheduled Polling Discipline

Performance MUST respect Vert.x's event-driven, non-blocking model
while supporting periodic telemetry fetching.

- NEVER block the Vert.x event loop with synchronous or CPU-heavy
  work.
- Use Vert.x worker threads or executeBlocking for unavoidable
  blocking operations.
- Telemetry fetching MUST be schedulable (configurable polling
  interval per tenant or globally).
- Set explicit limits for concurrency (max parallel EDC transfers),
  timeouts, and memory usage.
- Use backpressure-aware streaming for large telemetry payloads
  received from the EDC data plane.
- Measure before optimizing; avoid premature optimization.

---

### VIII. EDC Consumer Integration & Telemetry Fetching

The adapter MUST correctly integrate with an externally hosted
Eclipse Dataspace Connector as a consumer client.

- The adapter MUST use the EDC Management API to query the
  catalog of available assets from EDC Providers.
- Contract negotiation MUST follow the Dataspace Protocol (DSP)
  via the external EDC Consumer connector.
- Data transfers MUST be initiated only after successful contract
  agreement.
- The adapter MUST map dataspace assets to Hono device identities
  (tenant + device ID) to determine which telemetry to fetch.
- EDC policy evaluation results from the external Consumer MUST
  be respected and enforced.
- All EDC API interactions MUST use the official EDC Management
  API (REST); prefer the EDC client libraries if available.
- The adapter MUST handle EDC Consumer connector unavailability
  gracefully (circuit breaker, health degradation).

---

### IX. Observability & Operational Readiness

The adapter MUST be production-ready with full observability.

- Health check endpoints (/health/live, /health/ready) MUST be
  provided via Quarkus health extensions.
- The readiness check MUST verify connectivity to the external
  EDC Consumer connector.
- Prometheus-compatible metrics MUST be exposed for: adapter
  throughput, EDC fetch latency, contract negotiation success/
  failure rates, and Hono message forwarding rates.
- Distributed tracing (OpenTelemetry) MUST be supported.
- Structured logging with correlation IDs MUST link EDC data
  transfers to Hono device telemetry messages.

---

## Development Workflow

- Pull requests MUST be small, focused, and reviewable.
- Each PR MUST document **what changed**, **why**, and
  **how to test**.
- Refactors MUST NOT be mixed with feature work without
  justification.
- CI MUST pass (build, tests, static analysis) before merge.
- Reviews MUST explicitly verify compliance with this
  constitution.

---

## Documentation Standards

- Public APIs MUST be documented with Javadoc.
- Hono adapter configuration options MUST be documented in
  markdown.
- EDC integration data flow and asset-to-device mapping MUST be
  documented with PlantUML diagrams.
- Non-obvious design decisions require written rationale.
- Breaking changes MUST be documented clearly.

---

## Governance

This constitution supersedes all other conventions and practices
for the EDC adapter module.

- Deviations require explicit documentation and justification.
- All reviews MUST check for constitutional compliance.
- Amendments require:
  1. Written proposal with rationale
  2. Impact analysis and migration plan
  3. Maintainer approval
- Version follows semantic versioning:
  - MAJOR: Principle removals or redefinitions
  - MINOR: New principles or materially expanded guidance
  - PATCH: Clarifications, wording, typo fixes

**Version**: 3.0.0 | **Ratified**: 2026-03-09 | **Last Amended**: 2026-03-09
