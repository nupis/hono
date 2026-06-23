# Quickstart: EDC Data Ingestion Adapter

**Branch**: `001-edc-data-ingestion` | **Date**: 2026-03-09

## Prerequisites

- Java 21+ (LTS)
- Maven 3.9+
- Running Hono instance (Tenant, Device Registration, Credentials, Messaging services)
- Running EDC Consumer connector with Management API accessible
- Running EDC Provider connector with telemetry assets published
- Device(s) registered in Hono with device IDs matching EDC asset IDs

## Build

```bash
# From repository root
cd adapters/edc
mvn clean install
```

## Configuration

The adapter is configured via `application.yaml` or environment variables.

### Minimal Configuration (application.yaml)

```yaml
hono:
  edc:
    pollingInterval: 60s                             # 60-second polling interval
    managementApi:
      url: https://edc-consumer:8181                 # EDC Consumer Management API
      apiKey: ${HONO_EDC_MANAGEMENT_API_KEY}         # API key (from env var)
    providerUrl: https://edc-provider:8282/api/v1/dsp  # Provider DSP endpoint
    dataPlane:
      requestTimeout: 30s                            # Data plane pull timeout
```

### Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `HONO_EDC_MANAGEMENT_API_KEY` | API key for EDC Consumer Management API | Yes |
| `HONO_EDC_MANAGEMENT_API_URL` | EDC Consumer Management API base URL (must be HTTPS) | Yes |
| `HONO_EDC_PROVIDER_URL` | EDC Provider DSP endpoint | Yes |
| `HONO_EDC_TRUSTSTORE_PATH` | Path to TLS trust store for EDC endpoints | No |
| `HONO_EDC_TRUSTSTORE_PASSWORD` | Password for the TLS trust store | No |

### Standard Hono Configuration

The adapter inherits all standard Hono adapter configuration options for connecting to:
- AMQP messaging network or Kafka
- Tenant service
- Device Registration service
- Credentials service
- Command Router service

See [Hono documentation](https://www.eclipse.org/hono/docs/) for these settings.

## Run

```bash
# JVM mode
java -jar target/quarkus-app/quarkus-run.jar

# Or via Maven
mvn quarkus:dev
```

## Verify

1. **Health check**: `GET http://localhost:8080/q/health/ready` — should return UP when EDC Consumer is reachable.
2. **Metrics**: `GET http://localhost:8080/q/metrics` — look for `edc.adapter.*` metrics (e.g., `edc.adapter.cycles.total`, `edc.adapter.telemetry.forwarded`).
3. **Logs**: Watch for polling cycle logs with correlation IDs.

## End-to-End Test Scenario

1. Register a device in Hono: tenant=`my-tenant`, deviceId=`sensor-001`
2. Publish a telemetry asset in EDC Provider with asset ID=`sensor-001`
3. Start the EDC adapter with the configuration above
4. Wait for the first polling cycle (default: 60s)
5. Observe telemetry messages arriving on Hono's messaging infrastructure for `my-tenant/sensor-001`

## Troubleshooting

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| Readiness check DOWN | EDC Consumer unreachable | Verify `managementApi.url` and network connectivity |
| `management API URL must use HTTPS` | HTTP URL configured | Management API URL must start with `https://` |
| TLS handshake failure | Missing/invalid trust store | Set `HONO_EDC_TRUSTSTORE_PATH` and `HONO_EDC_TRUSTSTORE_PASSWORD` |
| No telemetry forwarded | Asset ID ≠ Device ID | Ensure EDC asset IDs exactly match Hono device IDs |
| Contract negotiation TERMINATED | Policy mismatch | Check EDC Provider policy definitions |
| `negotiation timed out` | EDC slow or unreachable | Increase `negotiation.timeout` or check EDC Provider health |
| Data plane pull timeout | Provider data plane unreachable | Verify EDR endpoint is accessible from adapter network |
| `polling cycle already in progress` | Cycle takes longer than interval | Increase `pollingInterval` or reduce number of assets |
