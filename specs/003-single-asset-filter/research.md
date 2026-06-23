# Research: Single Asset Filter

**Branch**: `003-single-asset-filter` | **Date**: 2026-03-10

## R1: EDC Catalog API — Server-Side Filtering Capability

**Decision**: Apply asset filtering in-memory after catalog query, not via EDC catalog API server-side filtering.

**Rationale**: The EDC Management API v3 catalog request supports a `querySpec` with `filterExpression` that can filter by asset properties. However, filtering by exact asset `@id` via `querySpec` requires knowledge of the EDC internal property path (`https://w3id.org/edc/v0.0.1/ns/id`), which varies across EDC versions and configurations. In-memory filtering after the catalog response is simpler, more portable across EDC versions, and already handles the paginated catalog response from the existing implementation.

**Alternatives considered**:
- EDC `querySpec.filterExpression` with `operandLeft: "https://w3id.org/edc/v0.0.1/ns/id"` → Rejected: couples the adapter to EDC internal property paths, which are not guaranteed stable across EDC versions. The catalog response is already paginated (max 100 assets), so in-memory filtering is negligible overhead.
- Separate catalog query per asset → Rejected: no EDC API endpoint for single-asset lookup by ID.

## R2: Quarkus Configuration Hot-Reload

**Decision**: Use standard Quarkus `@ConfigProperty` with programmatic configuration access for hot-reload support.

**Rationale**: Quarkus supports configuration refresh via MicroProfile Config. For the asset filter property, reading the configuration value at each polling cycle (rather than caching it at startup) ensures changes via environment variables, ConfigMaps, or Quarkus dev mode are picked up without restart. The `EdcAdapterProperties` class already follows a getter pattern that can delegate to the config source.

**Alternatives considered**:
- Static startup-only configuration → Rejected: FR-009 requires hot-reload support; operators should be able to change the filter without downtime.
- Quarkus `@ConfigMapping` with `@WithDefault` → Viable and cleaner, but the existing `EdcAdapterProperties` uses the traditional setter-based pattern. Consistency with existing code takes priority.

## R3: Asset Filter Configuration Property Name and Semantics

**Decision**: Property name: `hono.edc.assetIdFilter`. Type: `Optional<String>`. When absent or blank, all assets are processed.

**Rationale**: Follows the existing naming convention (`hono.edc.*`). Using `Optional<String>` makes the absence semantics explicit. The property is validated at startup: if present, it must be non-blank after trimming. An empty or whitespace-only value is treated as "no filter" (equivalent to absent).

**Alternatives considered**:
- `hono.edc.filter.assetId` (nested under `filter` namespace) → Rejected: over-engineering for a single filter property. If multi-criteria filtering is added later, the property can be deprecated in favor of a richer structure.
- `hono.edc.targetAssetId` → Rejected: less descriptive than `assetIdFilter` about its purpose.
- List/array type for multiple asset IDs → Rejected: spec explicitly scopes to "single asset filter". Keep the simplest type.

## R4: Filter Application Point in Polling Cycle

**Decision**: Filter immediately after catalog query returns, before `processAssetsSequentially()`.

**Rationale**: Applying the filter as early as possible in the pipeline avoids any unnecessary work (device resolution, contract negotiation, transfer). The filter is a simple predicate on `CatalogAsset.assetId()`. The existing `processAssetsSequentially()` method takes a `List<CatalogAsset>` — passing a filtered list requires zero changes to downstream processing logic.

**Implementation sketch**:
```
queryCatalog() → [all assets] → filterByAssetId() → [0 or 1 asset] → processAssetsSequentially()
```

**Alternatives considered**:
- Filter inside `processAssetsSequentially()` (per-asset skip) → Rejected: still iterates all assets, and the loop logging would be misleading.
- Filter at the `DeviceAssetMapper` level → Rejected: too late; contract negotiation may already have started for non-matching assets.

## R5: Logging Strategy for Filter Operations

**Decision**: Log filter status at three points: (1) adapter startup, (2) cycle start, (3) when filtered asset not found.

**Rationale**: These three log points provide complete operational visibility without excessive noise. Startup log confirms configuration was loaded correctly. Cycle-start log (with correlation ID) aids troubleshooting. Warning on "not found" alerts operators to catalog changes or misconfigurations.

**Log levels**:
- Startup: INFO — "Asset filter configured: assetId={}" or "No asset filter configured, processing all catalog assets"
- Cycle start: INFO — "Starting polling cycle [correlationId={}], asset filter: {}"
- Asset not found: WARN — "Filtered asset {} not found in catalog (discovered {} assets) [correlationId={}]"
