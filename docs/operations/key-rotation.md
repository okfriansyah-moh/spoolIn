# Protected Core key rotation

Run rotation from the installation operator account with its owned mode-0700 `SPOOLIN_LOCK_DIR` and established `SPOOLIN_INSTALLATION`. The script serializes against the other operator commands using the same installation lock. It invokes the packaged Core CLI through PropertiesLauncher in the actual Core image, private network and mounted configuration. It does not launch Gradle on the operator host.

Provision fresh independent 32-byte source/target material as protected mode-0600 files. Existing source versions must remain unchanged. Add both versions, with their exact purposes and container paths, to Core's protected `spoolin.account-protection.keys` configuration and the protected map selected by `ROTATION_KEYS_FILE` before rotation. Keep the source mounts and recovery copies. An alternate `--keys` argument to the native CLI must refer to that same configured map. Before touching database registration, the CLI loads Spring ConfigData and compares the purpose-bound source/target material against the application configuration. Missing or mismatched application mounts refuse activation.

The default command performs the actual plan's dry run, including mounted-material and database-state validation:

```sh
bash ops/bin/rotate-keys.sh --from old_draft --to next_draft --purpose DRAFT
```

Dry run does not register a missing target. It fails truthfully until the source/target registration is eligible. Registration is an explicit part of execution:

```sh
bash ops/bin/rotate-keys.sh --execute --register-target \
  --from old_draft --to next_draft --purpose DRAFT --max-batches 1
```

The script stops a currently running Core before registration/activation/backfill and starts it again on exit using the same mounted configuration. A Core that was already stopped remains stopped, allowing a coordinated operation across all purposes. The operator CLI is the only Core writer during this period. Requests routed through stopped Core are unavailable; serialize this operation with browser journeys. Do not run a separate Core instance against the same installation during rotation.

`--max-batches` accepts 1–10000, defaults to 100, and passes through to the existing rotation plan. A nonzero result with `remaining > 0` means activation succeeded but backfill remains incomplete. Resume with the same source/target IDs and purpose; do not provision replacement material under those IDs. A completed repeat reports zero newly backfilled rows. The algorithm keeps source READ and target WRITE; canary validation can safely consume a new nonce even when a repeat has no domain changes. Dry runs consume none.

Execution records immutable operator-bound authorization and result checkpoints in Core operational events. These describe actual command progress; they do not claim atomicity with the plan's separate activation/backfill transactions. A killed command without a result checkpoint remains unresolved. Dry run writes no operational events.

Errors are bounded and redact arguments, file contents and database/provider exception details. Command success is an actual structured rotation outcome. The script does not retire source keys. Zero live references alone is insufficient: retained backups must expire or be tested with their retained key material before retirement.

## Remote acceptance support

After the actual `make start` readiness, run the following sequentially on the verified GitHub-hosted Linux runner, before backup/restore verification and outside browser journeys:

```sh
python3 ops/ci/verify-key-rotation.py
```

This command refuses other installations/runners. It uses only the uniquely owned synthetic CI configuration and databases. It creates a draft through the real HTTPS API, provisions fresh protected keys for PII/DRAFT/EMAIL_LOOKUP while Core is stopped, updates the mounted application registry and reload map, and invokes the real script. It checks failed unregistered dry runs leave the database unchanged, actual activation/backfill, repeat-safe domain state, registered dry runs, source READ retention, target WRITE selection and dual email aliases. It restarts Core with the updated registry, verifies existing account/draft API decryption and original draft-receipt replay, then creates a new account and draft through the API and checks their target-key/alias metadata through independent database reads. TLS uses the configured CA and normal hostname validation.

Only bounded structured outcomes and a revision-bound summary go to `artifacts/key-rotation`; key material, cookies, CSRF values, passwords and raw protected API responses do not. On failure during provisioning/rotation, the verifier leaves Core stopped for diagnosis. Keep failed outcomes and protected installation state for the coordinator's corrective run. Do not upload the runtime configuration directory.

A small installation may finish within one batch. The report explicitly retains forced-interruption/resume proof with the real owner integration tests unless a run actually observes outstanding rows and resumes them. The helper does not certify backup retirement eligibility, visual/browser acceptance or a recovery drill. Those remain separate assigned proof boundaries. Source changes and the existence of this helper are not runtime acceptance.
