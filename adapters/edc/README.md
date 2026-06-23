# Hono EDC Adapter

> **Remark:** The development was implemented as part of the IPCEI-CIS funding initiative (project GREEN-Twin).

A [Quarkus](https://quarkus.io/)-based Eclipse Hono™ protocol adapter that periodically
consumes telemetry data from an [Eclipse Dataspace Connector (EDC)](https://eclipse-edc.github.io/)
and forwards it into Hono's messaging infrastructure.

It acts on the **consumer side** of a dataspace: each polling cycle queries the provider's
catalog, performs the Dataspace Protocol handshake (contract negotiation + `HttpData-PULL`
transfer), pulls the data via the EDC Data Plane, maps each asset 1:1 to a Hono device
(`assetId == deviceId`), and publishes the payload as a Hono telemetry message.

- **Maven artifact:** `org.eclipse.hono:hono-adapter-edc`
- **Adapter type:** `hono-edc` (dataspace consumer, EDC → Hono)
- **License:** Eclipse Public License 2.0 (EPL-2.0)

The adapter exposes no device-facing port; it runs a periodic polling loop and additionally
needs the standard Hono service connections (messaging, tenant, device registration).

## Configuration

Settings live under the `hono.edc` prefix. Required:

| Property (`hono.edc.*`) | Environment variable | Description |
| --- | --- | --- |
| `managementApi.url` | `HONO_EDC_MANAGEMENT_API_URL` | EDC Consumer Management API base URL (**HTTPS**). |
| `managementApi.apiKey` | `HONO_EDC_MANAGEMENT_API_KEY` | Management API key. |
| `providerBpn` | `HONO_EDC_PROVIDER_BPN` | Business Partner Number of the EDC provider. |
| `providerUrl` | `HONO_EDC_PROVIDER_URL` | EDC provider DSP endpoint URL. |

Optional: `assetIdFilter` (`HONO_EDC_ASSET_ID_FILTER`, restrict to one asset),
`pollingInterval` (default `60s`, min `10s`), and the negotiation/transfer/retry timeouts.
TLS truststore via `HONO_EDC_TRUSTSTORE_PATH` / `HONO_EDC_TRUSTSTORE_PASSWORD`.
See `src/main/resources/application.yaml` for the full set and defaults.

## Build & run

```bash
# Build this module (from the repository root)
mvn install -pl adapters/edc -am

# Container image: add -Pbuild-docker-image (JVM) or -Pbuild-native-image (native)

# Run
export HONO_EDC_MANAGEMENT_API_URL="https://edc-consumer.example.com/management"
export HONO_EDC_MANAGEMENT_API_KEY="<api-key>"
export HONO_EDC_PROVIDER_URL="https://edc-provider.example.com/api/dsp"
export HONO_EDC_PROVIDER_BPN="BPNL000000000000"
java -jar adapters/edc/target/hono-adapter-edc-*-runner.jar
```

A SmallRye Health readiness probe (`EDC Consumer connectivity`) reports UP when the EDC
Management API was reachable during the last polling cycle, DOWN otherwise.

## License & acknowledgement

Part of Eclipse Hono™, published under the [Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0/) (`SPDX-License-Identifier: EPL-2.0`).

**Remark:** The development was implemented as part of the IPCEI-CIS funding initiative (project GREEN-Twin).

- Eclipse Hono™ (N+P fork): <https://github.com/nupis/hono>
- CISERO OSS Discovery Point: <https://cisero-project.eu/oss-discovery-point/edc-adapter-eclipse-hono-np-digital-services>
