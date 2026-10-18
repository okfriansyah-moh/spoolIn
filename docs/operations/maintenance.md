# Owner retention maintenance

`ops/bin/maintenance.sh --service core --schedule hourly` inspects the selected
owner connection with its protected maintenance credential and verified database
TLS. It reports `executed: false` and performs no retention mutation. Select
`core`, `catalog` or `media` explicitly; no owner is inferred.

Add `--execute` to run one bounded hourly batch, or choose `--schedule daily` for
one bounded daily pass. The packaged owner image invokes the same
`RetentionMaintenance` routines used by scheduled maintenance. Each selection
uses the existing 100-row limits and eligibility checks; it preserves receipt
identities, unresolved work, paired attempt evidence and retained asset outputs.
The result contains owner, schedule, execution status and aggregate counts.

Compose supplies `<OWNER>_DB_URL` with `sslmode=verify-full`, a trusted CA and
`<OWNER>_DB_MAINTENANCE_PASSWORD_FILE`. The shared factory selects exactly
`spoolin_<owner>_maintenance`, verifies the actual database role and uses bounded
statement and lock timeouts. Runtime, owner and migrator passwords cannot replace
this credential. No DDL, VACUUM or REINDEX is part of this retention command.
Unknown, duplicate or missing options fail. The CLI exits 2 for invalid
configuration and 1 for operational failures, with a redacted diagnostic. The
wrapper returns 78 when its required Compose environment is unavailable.

`ops/bin/g04-g16-task-sweep.sh` selects T006–T026 unit and integration suites from
`build-tools/verification-map.json` and runs them serially only on GitHub-hosted
Linux. Missing mappings are explicit unavailable obligations and exit 2 before
execution. A passing nearby suite cannot fill a missing mapping. The report at
`.ai/evidence/numbering-v1/g04-g16-task-sweep.json` retains the assessed SHA,
workflow/run identity, image version, mapping hash, selected tasks, actual suite
counts, results and unavailable reasons. Timestamped report copies preserve
earlier failures alongside later invocations. It is an orchestration record; assigned
browser, client, database and other acceptance boundaries still require their
own assertions and reports.
