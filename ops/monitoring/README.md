# Private monitoring integration

These files implement the monitoring portion of the approved startup DAG. They require the real Core, Catalog, Media and MCP application; they supply no alternate application or mocked APIs. Execution and image builds belong on owned remote CI runners.

Merge `compose-services.yaml` services, secrets and volumes into `ops/compose.yaml`; paths in the fragment are relative to that file. The fragment is an integration source, not a second launcher. Do not load it through Compose `include`, which would duplicate shared private-network resources. Start the merged stack through normal `make start`; status and stop must include these selected services.

The derived Dockerfile targets retain the approved upstream versions and immutable multi-platform indexes recorded in `images.lock.json`. They initialize only their own data directories to UID10001 so new named volumes support nonroot services and protected secret mounts. Reused volumes retain existing ownership; incompatible existing installations require an explicit owned-volume migration, not a root service or data deletion. Preserve the three named monitoring volumes across stop/restart and checkpoint them if their operational-history retention is part of recovery scope.

## Protected configuration and trust

Provision distinct service-CA-signed identities and protected files before startup:

| Setting | Requirement |
| --- | --- |
| `PROMETHEUS_SERVICE_KEY_FILE`, `PROMETHEUS_SERVICE_CERT_FILE` | Client key/cert, exact DNS SAN `prometheus` |
| `ALERTMANAGER_SERVICE_KEY_FILE`, `ALERTMANAGER_SERVICE_CERT_FILE` | Client key/cert, exact DNS SAN `alertmanager`; no localhost SAN |
| `GRAFANA_SERVICE_KEY_FILE`, `GRAFANA_SERVICE_CERT_FILE` | HTTPS key/cert with localhost and grafana DNS names |
| `GRAFANA_ADMIN_PASSWORD_FILE` | Distinct protected random operator password mounted into Grafana |
| `GRAFANA_SIGNING_KEY_FILE` | Distinct stable protected random signing secret, retained across restart |
| `GRAFANA_ADMIN_PASSWORD_CLIENT_FILE` | Runner-owned0600 copy of the same password, used only by remote verification |
| `GRAFANA_HTTPS_PORT` | Loopback host port, default3000; use a distinct restore port |

Container key/password files must be0600 and owned by UID10001. The runner-side client copy must be0600 and remain outside uploaded evidence. Grafana serves HTTPS on loopback only, requires authentication, disables signup/anonymous access and uses Secure/SameSite cookies. Prometheus and Alertmanager publish no host port. Their APIs use the private Compose network; JVM scrapes and the Core alert receiver use verified mTLS with separate sender identities. Never forward monitoring through the MCP tunnel.

## JVM wiring

The three service builds require the Boot-managed `micrometer-registry-prometheus` runtime dependency; backend-support requires `micrometer-core`. Expose `health,prometheus` only on the existing private management listener. Retain the exact certificate/path authorization: Prometheus may scrape; Alertmanager may POST only to `/internal/alerts`; ordinary MCP/browser identities cannot use either path.

Create one `OwnerMetrics` for each service using its actual runtime datasource and MeterRegistry. Supply real Hikari active/max connection callbacks and actual dependency-readiness callback. Media can pass its owned `/var/spoolin/media` directory as volume `artwork`. Do not pass fabricated counts, nonexistent volume paths or substitute filesystem mounts for the database/backup volume. Those volume observations require their actual owning collectors.

Serialize each Prometheus request using `synchronized(metrics)`, call `metrics.prepareScrape()` before rendering, and return HTTP503 when it returns false. Keep the lock through the Actuator endpoint response. Collection uses a read-only, repeatable-read PostgreSQL snapshot with two-second statement limits. Empty measured groups are zero; a failed measurement invalidates the snapshot and cannot become a healthy zero-valued scrape. `up` reports exporter reachability separately from `spoolin_service_ready`. Database-only notification recordings are excluded from worker-lifecycle counts, while every notification state remains separately visible.

The shared collector implements work states, oldest due age, notification state counts, pool usage, readiness and explicitly mounted owned-volume space. It does not invent logical outcomes, operation durations, crypto failures, processor outcomes, logging-drop counts or tunnel observations. Their production boundary instruments remain required. Empty dashboard panels disclose absent series; no `or vector(0)` fallback hides a missing instrument. The Core and Catalog existing `spoolin_work_pending` gauges have different described meanings and separate dashboard panels. Seed/migration details and retry histories still require their own actual observations; readiness alone does not establish initialization acceptance.

## Fixed alert policy

Core's exact allowed names and summaries must match the following source-owned list. Class labels are fixed to availability, work, mutation, notification, database, disk, crypto, media and tunnel. Group service+class, wait30s, repeat4h, and persist resolved episodes. Notification-class failures must be recorded in database mode only to prevent recursion through a failing external adapter. This restriction does not make other selected external-provider obligations optional.

| Name | Summary |
| --- | --- |
| ServiceUnavailable | Selected service unavailable |
| WorkQueueBacklog | Durable work backlog elevated |
| SystemMutationFailure | System mutation failure ratio elevated |
| ExhaustedWork | Work requires controlled recovery |
| DelayedWork | Due work delayed |
| NotificationUnresolved | Notification requires reconciliation |
| PoolPressure | Database pool pressure elevated |
| DiskWarning | Owned volume headroom low |
| DiskCritical | Owned volume headroom critical |
| CryptoFault | Cryptographic protection failure |
| ProcessingFailures | Media processing failure ratio elevated |
| TunnelUnavailable | Selected tunnel disconnected |

## Remote assertions and evidence

After real `make start` readiness, run `python3 ops/monitoring/verify.py --report artifacts/monitoring.json` on GitHub-hosted Linux. The script refuses other environments, operates only an owned `ci-` installation and writes bounded JSON results with SHA/run/runner/configuration hashes. It retains failed results and returns nonzero if any assertion fails. The coordinator uploads and inspects this report with the composed acceptance artifacts.

The verifier executes the pinned promtool/amtool, checks real container isolation, waits for all three actual JVM and the MCP exporter scrapes, queries measured series, checks mTLS identity denial, and uses authenticated Grafana APIs to verify its provisioned datasource/dashboard. It injects an explicitly synthetic Alertmanager episode through the real Alertmanager API, waits for the actual mTLS Core receiver and PostgreSQL evidence, verifies duplicate coalescing, then resolves the same episode and checks the separate retained resolution. This is transport/dedup proof; rule evaluation uses the separate promtool assertions, and an API response is not a rendered browser observation. The script neither changes application data to imitate UI success nor publishes credentials.

The rule tests cover missing series, low traffic, domain failures, success-counter reset, sustained mutation failure, pool pressure, unresolved notifications, disabled tunnel and resolved backlog. Required external-provider failure/retry observations and complete logical-boundary assertions remain separate requirements.

Rendered dashboard screenshots/accessibility observations must come from the remote real browser. All required logical metric families need actual source instruments and runtime proof. No registered tunnel credential or assisted-client session is supplied by this stack; configured tunnel startup, connected-state observation and both actual host approval journeys remain outstanding until their real prerequisites are available.

Primary configuration references: [Prometheus3.15 configuration](https://prometheus.io/docs/prometheus/3.15/configuration/configuration/), [Alertmanager configuration](https://prometheus.io/docs/alerting/latest/configuration/), [Grafana configuration](https://grafana.com/docs/grafana/latest/setup-grafana/configure-grafana/). Registry digests were read from Docker Hub's official authenticated manifest API without local image execution.
