# Quickstart: Single Asset Filter

**Branch**: `003-single-asset-filter` | **Date**: 2026-03-10

## Prerequisites

- Existing EDC adapter deployment (see [001-edc-data-ingestion quickstart](../001-edc-data-ingestion/quickstart.md))
- At least one asset published in the EDC Provider catalog
- Corresponding device registered in Hono

## Configuration

### Enable Asset Filter

Add the `assetIdFilter` property to your existing configuration.

#### application.yaml

```yaml
hono:
  edc:
    pollingInterval: 60s
    assetIdFilter: sensor-042                           # Only poll this asset
    managementApi:
      url: https://edc-consumer:8181
      apiKey: ${HONO_EDC_MANAGEMENT_API_KEY}
    providerUrl: https://edc-provider:8282/api/v1/dsp
```

#### Environment Variable

```bash
export HONO_EDC_ASSET_ID_FILTER=sensor-042
```

### Disable Asset Filter (Process All Assets)

To process all assets (default behavior), either:
- Omit the `assetIdFilter` property entirely
- Set it to an empty value: `HONO_EDC_ASSET_ID_FILTER=`

## Verify

1. **Check startup logs** for filter confirmation:
   ```
   INFO  Asset filter configured: assetId=sensor-042
   ```
   Or, if no filter:
   ```
   INFO  No asset filter configured, processing all catalog assets
   ```

2. **Check polling cycle logs** for filtered processing:
   ```
   INFO  Starting polling cycle [correlationId=abc-123], asset filter: sensor-042
   INFO  Discovered 15 assets in catalog, processing 1 after filtering
   ```

3. **Check for warnings** if the asset is not in the catalog:
   ```
   WARN  Filtered asset sensor-042 not found in catalog (15 assets discovered) [correlationId=abc-123]
   ```

## Change Filter at Runtime

Update the configuration source (environment variable, ConfigMap, or Quarkus dev mode) and the next polling cycle will use the new value. No restart required.

```bash
# Switch to a different asset
export HONO_EDC_ASSET_ID_FILTER=sensor-099

# Remove filter (process all)
unset HONO_EDC_ASSET_ID_FILTER
```

## Troubleshooting

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| No telemetry after setting filter | Asset ID typo or asset not in catalog | Check WARN log for "not found"; verify asset ID matches EDC catalog exactly |
| All assets still processed | Filter not applied | Verify startup log shows "Asset filter configured"; check property name spelling |
| `Invalid asset filter` at startup | Whitespace-only filter value | Set a valid asset ID or remove the property entirely |
| Filter change not picked up | Configuration source not refreshable | Ensure using environment variables or a refreshable config source |
