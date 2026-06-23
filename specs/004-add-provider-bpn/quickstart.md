# Quickstart: Add Provider BPN

**Branch**: `004-add-provider-bpn` | **Date**: 2026-03-10

## Prerequisites

- Existing EDC adapter deployment (see [001-edc-data-ingestion quickstart](../001-edc-data-ingestion/quickstart.md))
- The BPN of the EDC Provider you're connecting to (e.g., "BPNL00000003CRHK")

## Configuration

### Add Provider BPN

The provider BPN is a **required** configuration property.

#### application.yaml

```yaml
hono:
  edc:
    pollingInterval: 60s
    providerBpn: BPNL00000003CRHK                      # Provider's Business Partner Number
    managementApi:
      url: https://edc-consumer:8181
      apiKey: ${HONO_EDC_MANAGEMENT_API_KEY}
    providerUrl: https://edc-provider:8282/api/v1/dsp
```

#### Environment Variable

```bash
export HONO_EDC_PROVIDER_BPN=BPNL00000003CRHK
```

### Migration from Previous Version

**BREAKING CHANGE**: The adapter now requires `providerBpn` to be configured. Without it, the adapter will fail to start.

```bash
# Previously (no BPN needed):
export HONO_EDC_MANAGEMENT_API_URL=https://edc-consumer:8181
export HONO_EDC_MANAGEMENT_API_KEY=secret
export HONO_EDC_PROVIDER_URL=https://edc-provider:8282/api/v1/dsp

# Now (BPN required):
export HONO_EDC_MANAGEMENT_API_URL=https://edc-consumer:8181
export HONO_EDC_MANAGEMENT_API_KEY=secret
export HONO_EDC_PROVIDER_URL=https://edc-provider:8282/api/v1/dsp
export HONO_EDC_PROVIDER_BPN=BPNL00000003CRHK           # NEW - required
```

## Verify

1. **Check startup logs** for BPN confirmation:
   ```
   INFO  EDC protocol adapter starting [providerBpn=BPNL00000003CRHK]
   ```

2. **Check that startup fails** without BPN:
   ```
   ERROR EDC provider BPN must be configured
   ```

3. **Verify catalog query** includes BPN (in DEBUG logs):
   ```
   DEBUG querying EDC catalog [providerDspUrl=..., providerBpn=BPNL00000003CRHK]
   ```

## Finding the Provider's BPN

The BPN is typically provided by:
- The dataspace onboarding process
- The provider's connector registration
- The dataspace portal/marketplace (e.g., Catena-X Portal)

If unknown, contact the provider or dataspace operator.

## Troubleshooting

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| Adapter fails to start with "provider BPN must be configured" | Missing `providerBpn` property | Set `HONO_EDC_PROVIDER_BPN` environment variable |
| Catalog query returns empty results | Wrong BPN | Verify the BPN matches the provider's registered identity |
| Negotiation rejected with policy error | BPN not authorized | Check provider's access policies for your BPN |
| `Invalid BPN` log at startup | Whitespace-only value | Set a valid non-blank BPN |
