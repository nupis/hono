<!--
Sync Impact Report
===================
Version change: 1.0.0 → 2.0.0 (MAJOR - complete redefinition for new project domain)
Modified principles:
  - I. Clarity Over Cleverness → I. Eclipse Hono Adapter Compliance
  - II. Explicit Architecture & Boundaries → II. Hexagonal Architecture & Clean Boundaries
  - III. Modern Node.js → III. Modern Java & Vert.x/Quarkus Usage
  - IV. Test-First Development → IV. Test-First Development (retained, adapted)
  - V. Error Handling → V. Error Handling & Input Validation (adapted)
  - VI. Tooling → VI. Tooling, Formatting & Static Analysis (adapted)
  - VII. Security by Default → VII. Security & Data Sovereignty
  - VIII. Performance → VIII. Performance & Event Loop Discipline (adapted for Vert.x)
Added sections:
  - Principle IX: EDC Integration & Dataspace Compliance
  - Principle X: Observability & Operational Readiness
Removed sections:
  - Node.js 24 constitution (entire block)
  - .NET 10 constitution (entire block)
  - Language agnostic settings (superseded)
Templates requiring updates:
  - .specify/templates/plan-template.md: ⚠ pending (Constitution Check references generic gates)
  - .specify/templates/spec-template.md: ✅ compatible (no constitution-specific references)
  - .specify/templates/tasks-template.md: ✅ compatible (no constitution-specific references)
Follow-up TODOs: none
-->

# Hono EDC Adapter Constitution

## Core Principles

### I. Eclipse Hono Adapter Compliance (NON-NEGOTIABLE)

The adapter MUST conform to Eclipse Hono's protocol adapter contract.

- The adapter MUST implement Hono's standard adapter interfaces
  (telemetry, event, command & control messaging patterns).
- Device authentication MUST use Hono's Credentials API.
- Device identity MUST be resolved via Hono's Device Registration API.
- The adapter MUST support the device gateway pattern for
  non-IP-capable devices.
- TTD (time-till-disconnect) notifications MUST be forwarded
  correctly to the Command Router.
- All Hono service client interactions (Tenant, Registration,
  Credentials, Command Router) MUST follow Hono's documented contracts.

---

### II. Hexagonal Architecture & Clean Boundaries (NON-NEGOTIABLE)

The system MUST enforce strict separation between domain logic,
Hono adapter infrastructure, and EDC integration.

- Domain logic MUST NOT depend on Vert.x, Quarkus, or Hono framework
  classes directly.
- Hono adapter concerns (protocol translation, device auth) form
  an inbound port/adapter pair.
- EDC integration (contract negotiation, data plane transfer) forms
  an outbound port/adapter pair.
- Modules MUST expose clear, minimal public APIs.
- No circular dependencies between modules.

---

### III. Modern Java & Vert.x/Quarkus Usage

The codebase MUST leverage current Java, Vert.x, and Quarkus
capabilities.

- Java 21+ (LTS) with modern language features (records, sealed
  classes, pattern matching) is required.
- Vert.x async/reactive patterns MUST be used consistently;
  no blocking calls on the event loop.
- Quarkus is the target runtime; use Quarkus extensions and CDI
  for dependency injection.
- Prefer immutable data structures (Java records) unless mutation
  is explicitly justified.
- Maven is the build system; follow Hono's multi-module conventions.
- Native image compilation MUST remain feasible; avoid reflection
  where possible or register it explicitly.

---

### IV. Test-First Development (NON-NEGOTIABLE)

Tests define behavior and protect design.

- Tests MUST be written before or alongside implementation.
- Hono adapter protocol compliance requires integration tests against
  Hono's test utilities.
- EDC contract negotiation and data transfer require integration tests.
- Domain logic requires unit tests with deterministic, isolated,
  fast execution.
- Bugs require a failing test before a fix is accepted.

---

### V. Error Handling & Input Validation

Errors MUST be explicit, structured, and observable.

- Validate all device input at the adapter boundary.
- Use Hono's standard error response codes for device-facing errors.
- EDC negotiation failures MUST be logged with correlation IDs
  and propagated as structured errors.
- Never swallow exceptions; always log with sufficient context.
- Vert.x Future/Promise rejections MUST always be handled.

---

### VI. Tooling, Formatting & Static Analysis (MANDATORY)

Automation and consistency are non-optional.

- Code formatting MUST be enforced via a consistent formatter
  (e.g., Spotless with Google Java Style or Eclipse formatter).
- Static analysis (SpotBugs, Error Prone, or equivalent) MUST be
  enabled with zero new warnings.
- Structured logging (SLF4J + JSON layout) is mandatory; no secrets
  or PII in logs.
- Dependencies MUST be minimal, justified, and regularly updated.
- All builds MUST pass linting, compilation, and tests before merge.

---

### VII. Security & Data Sovereignty (NON-NEGOTIABLE)

Security and data sovereignty are baseline requirements, not
enhancements.

- Secrets MUST never be committed to source control.
- Device credentials MUST be validated via Hono's Credentials API;
  no custom credential stores.
- EDC policy enforcement MUST be respected; data MUST NOT be
  transferred without a valid contract agreement.
- All data exchange MUST comply with dataspace policies
  (usage policies, access control, purpose limitation).
- TLS MUST be enforced for all external communication channels.
- Apply least-privilege principles for all credentials and
  service accounts.

---

### VIII. Performance & Event Loop Discipline

Performance MUST respect Vert.x's event-driven, non-blocking model.

- NEVER block the Vert.x event loop with synchronous or CPU-heavy work.
- Use Vert.x worker threads or executeBlocking for unavoidable
  blocking operations.
- Set explicit limits for concurrency, timeouts, and memory usage.
- Use backpressure-aware streaming for large telemetry payloads.
- Measure before optimizing; avoid premature optimization.

---

### IX. EDC Integration & Dataspace Compliance

The adapter MUST correctly integrate with Eclipse Dataspace Connector.

- Contract negotiation MUST follow the Dataspace Protocol (DSP).
- The adapter MUST support EDC's extension model for pluggable
  data plane implementations.
- Data transfers MUST be initiated only after successful contract
  agreement.
- EDC policy evaluation results MUST be respected and enforced.
- The adapter MUST support both push and pull data transfer patterns
  as defined by the EDC data plane.
- All EDC API interactions MUST use the official EDC client libraries
  or Management API.

---

### X. Observability & Operational Readiness

The adapter MUST be production-ready with full observability.

- Health check endpoints (/health/live, /health/ready) MUST be
  provided via Quarkus health extensions.
- Prometheus-compatible metrics MUST be exposed for adapter
  throughput, latency, error rates, and EDC negotiation metrics.
- Distributed tracing (OpenTelemetry) MUST be supported.
- Structured logging with correlation IDs MUST link device
  requests to EDC data transfers.

---

## Development Workflow

- Pull requests MUST be small, focused, and reviewable.
- Each PR MUST document **what changed**, **why**, and **how to test**.
- Refactors MUST NOT be mixed with feature work without justification.
- CI MUST pass (build, tests, static analysis) before merge.
- Reviews MUST explicitly verify compliance with this constitution.

---

## Documentation Standards

- Public APIs MUST be documented with Javadoc.
- Hono adapter configuration options MUST be documented in markdown.
- EDC extension points and data flow MUST be documented with
  PlantUML diagrams.
- Non-obvious design decisions require written rationale.
- Breaking changes MUST be documented clearly.

---

## Governance

This constitution supersedes all other conventions and practices.

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

**Version**: 2.0.0 | **Ratified**: 2026-03-09 | **Last Amended**: 2026-03-09
