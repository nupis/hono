# API Contracts: Single Asset Filter

**Branch**: `003-single-asset-filter` | **Date**: 2026-03-10

## No New External Contracts

This feature does not introduce new API endpoints or modify existing EDC Management API interactions. The asset filter is an internal configuration and processing concern.

### Unchanged Contracts

All EDC Management API contracts from [001-edc-data-ingestion](../../001-edc-data-ingestion/contracts/edc-management-api-usage.md) remain unchanged:

- **Catalog Query**: Same `POST /management/v3/catalog/request` — no server-side filtering added
- **Contract Negotiation**: Unchanged — only invoked for filtered assets
- **Transfer Process**: Unchanged
- **EDR Retrieval**: Unchanged
- **Data Plane Pull**: Unchanged

### Internal Contract: Configuration Property

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `hono.edc.assetIdFilter` | String (optional) | absent | When set, restricts polling to this single asset ID |

**Environment variable equivalent**: `HONO_EDC_ASSET_ID_FILTER`
