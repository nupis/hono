# Data Model: Single Asset Filter

**Branch**: `003-single-asset-filter` | **Date**: 2026-03-10

## Overview

The single asset filter feature adds one optional configuration property and a filtering step to the existing polling cycle. No new entities are introduced. The filter operates on the existing `CatalogAsset` list returned by the catalog query.

## Modified Entities

### EdcAdapterProperties (Configuration) — Extended

New field added to the existing configuration record.

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| assetIdFilter | String (nullable) | No | null (no filter) | When set, only the asset with this exact ID is processed. When absent/blank, all assets are processed. |

**Validation rules**:
- If present and non-null, must be non-blank after trimming
- Whitespace-only values are treated as absent (no filter)
- No character restrictions beyond non-blank (asset IDs are opaque strings from EDC)

**Configuration path**: `hono.edc.assetIdFilter`

**Environment variable**: `HONO_EDC_ASSET_ID_FILTER`

### CatalogAsset (Unchanged)

No changes. The existing `CatalogAsset` record with fields `assetId`, `offerId`, `providerDspUrl` is used as-is. Filtering is applied as a predicate on `assetId`.

## New Behavior: Asset Filtering Step

Inserted into the existing polling cycle between catalog query and asset processing.

```
Existing flow:
  queryCatalog() → [CatalogAsset list] → processAssetsSequentially()

New flow:
  queryCatalog() → [CatalogAsset list] → filterAssets() → [filtered list] → processAssetsSequentially()
```

### filterAssets() Logic

```
Input: List<CatalogAsset> assets, Optional<String> assetIdFilter

IF assetIdFilter is absent or blank:
  RETURN assets (unmodified — backward compatible)

filtered = assets.stream()
  .filter(a -> a.assetId().equals(assetIdFilter.get()))
  .toList()

IF filtered is empty:
  LOG.warn("Filtered asset {} not found in catalog ({} assets discovered)")

RETURN filtered (0 or 1 element)
```

## Relationships

```
EdcAdapterProperties
    └── assetIdFilter (optional) ─── applied by ──→ TelemetryPollingService.filterAssets()

TelemetryPollingService (per polling cycle)
    ├── queries → EDC Catalog → [CatalogAsset list]
    ├── filters → filterAssets(assets, assetIdFilter) → [0 or 1 asset]
    └── processes → processAssetsSequentially(filtered) → (existing flow)
```

## Impact Assessment

| Component | Change Required |
|-----------|----------------|
| EdcAdapterProperties | Add `assetIdFilter` field, getter, setter, validation |
| TelemetryPollingService | Add `filterAssets()` method, call it in `executeCycle()` |
| EdcProtocolAdapter | No changes (properties are already passed through) |
| EdcManagementClient | No changes (catalog query unchanged) |
| DeviceAssetMapper | No changes (only sees filtered assets) |
| application.yaml | Add optional `assetIdFilter` property example |
