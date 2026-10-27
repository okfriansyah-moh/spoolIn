# Private monitoring integration

These files implement the monitoring portion of the approved startup DAG. They require the real Core, Catalog, Media and MCP application; they supply no alternate application or mocked APIs. Deployment acceptance runs on owned remote CI runners. The optional local installation described below builds and runs its monitoring containers on the local Docker engine.

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

## Optional local monitoring

For the installation created by `make local-start`, run:

```sh
make local-monitoring
```

This starts Prometheus, Alertmanager, Grafana, Loki, Alloy, cAdvisor and a PostgreSQL exporter alongside the existing app. It does not rebuild or restart application containers, rerun migrations, replace existing identities, or reset data. MCP monitoring becomes available when `make local-mcp` is running. An absent optional MCP target stays visibly down.

Open [Grafana](https://localhost:3000) and sign in as `operator`. The password is in the protected `.local/monitoring.txt` file. `make local-trust` installs this installation's CA for browser access. Keep credential files private. Set `GRAFANA_HTTPS_PORT=3300 make local-monitoring` to use another loopback port; the selected port is retained for subsequent starts. Existing Grafana data retains its password; changing a password file does not reset an existing Grafana account.

Four provisioned dashboards appear in the **SpoolIn** folder: operations, Docker infrastructure, PostgreSQL, and logs. Use the service/database filters and common time picker. Logs also offer level and text filters; opening log details preserves the structured JSON fields, including safe correlation fields when emitted. No request ID, trace ID, email or arbitrary resource value becomes an indexed label. Missing metrics remain empty; a successful scrape does not imply every container metric is available.

```sh
make local-status
make local-logs
make local-monitoring-stop
```

The monitoring stop command leaves the app and all history volumes intact. `make local-monitoring` resumes it. `make local-stop` stops the complete local installation and preserves volumes. `make local-reset` retains its existing explicit confirmation and deletes the complete local installation, including monitoring history.

### Collection and storage

The local renderer extends the existing deployment monitoring services and derives local Prometheus/Alertmanager files from their source configurations. Extra local scrape jobs and infrastructure rules are separate; infrastructure alerts route to an empty local receiver and stay visible in Prometheus. Existing application alert routing to Core is retained. No external notification integration is installed. Rules identify the local operator, provide recovery actions, and use sustained windows to reduce startup noise; missing container metrics have a separate alert. No absent measurement is replaced with a healthy zero.

Prometheus retains metrics for 15 days with the existing 1 GB size limit. Loki retains logs for 72 hours, with asynchronous deletion after the retention delay. This is a time limit, not a filesystem quota: inspect Docker's available disk space and stop collection before exhausting it. Loki limits ingestion to 1 MB/s with a 2 MB burst, caps line length at 16 KiB and limits query concurrency and returned entries. Alloy limits collection to 100 lines/s, drops excess entries, retries delivery five times with bounded backoff and retains collection offsets. Log collection is best effort and can have gaps or replay after outages; it is not an audit archive.

Alloy discards lines containing credential indicators, connection URLs or email addresses before sending them to Loki. This intentionally sacrifices some diagnostics to reduce sensitive storage. Application log redaction remains necessary: filtering cannot identify every sensitive free-text value. The logs dashboard shows dropped-line counts. Docker logs rotate at 10 MB across three files for newly created containers; existing application containers adopt this setting during their next normal recreation, avoiding a monitoring-triggered app restart.

Only the log proxy holds the collection socket, on an isolated internal network shared with Alloy. It allows GET container/network discovery, version/ping and project-owned container logs/TTY/lifecycle status, forces the installation filter, strips unnecessary metadata and strips environment, commands and mounts from inspection responses, and denies filesystem, exec and write APIs. Log streams have bounded backlog, bytes, idle timeout and concurrent connections. A read-only socket mount alone does not restrict Docker API operations; the proxy enforces the restriction. The proxy process itself is trusted because its socket access can control Docker if compromised.

cAdvisor is a separate trusted infrastructure collector. It requires root and read-only Linux host/cgroup/Docker mounts to discover resource usage; these mounts and Docker socket access are powerful even with dropped capabilities. Alloy never receives those mounts. Prometheus retains only metrics labeled for `spoolin-local`. On Docker Desktop, these observations describe containers in its Linux VM, not native macOS resources. Filesystem observations vary by Docker storage driver; validate actual series before relying on them.

The PostgreSQL exporter uses a separate read-only `spoolin_monitor` login with `pg_monitor`, a three-connection limit, a three-second statement timeout and verified TLS. It has no application table grants or role/database creation privileges. `pg_monitor` can read database statistics and settings; this is a trusted monitoring identity. Statement-text collection is explicitly disabled, and passwords are mounted files rather than environment values or command arguments.

### Troubleshooting

- **Port occupied:** choose `GRAFANA_HTTPS_PORT` and rerun the monitoring command.
- **Certificate error:** install the local CA; inspect expiry instead of replacing the installation. The launcher creates missing monitoring identities but refuses incomplete key/certificate pairs.
- **Empty logs:** inspect Alloy/proxy/Loki health and the selected time range. Only running project containers are discovered; sensitive lines are deliberately dropped.
- **Empty Docker panels:** inspect cAdvisor health and mounts. Docker Desktop or a storage driver may expose only part of the Linux resource data. Collector reachability and actual container series are separate checks.
- **Database panel down:** check exporter logs, PostgreSQL readiness, monitoring-role access and the CA mount. Rerunning the command reapplies the monitoring role without touching application schemas.
- **Resource pressure:** do not add concurrent builds or collectors while the execution-device admission check is blocked. Container limits bound individual services; they do not establish host capacity.
