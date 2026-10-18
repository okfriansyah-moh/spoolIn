# Codec runner AppArmor prerequisite

This correction is source-only until remote execution supplies the retained results. It does not close T044 / ATDD-250, prove host execution, or prove the production container sandbox.

## Observed failure

GitHub-hosted run **39093635908**, attempt **1**, assessed **890ff3d807fb2c90e6adf102495b836fe6d0daf2**, used Ubuntu 24.04 image **20261012.334.1** and kernel **6.17.0-1022-azure**. The retained `codec-host-after.json` and `codec-sandbox-preflight.log` show the non-root caller had no effective capabilities and host seccomp mode 0. The `unprivileged_userns` AppArmor profile denied `net_admin` and `setpcap`; bubblewrap failed its loopback `RTM_NEWADDR` operation. `apparmor_restrict_unprivileged_userns` remained 1. No loaded bwrap profile was recorded. The filename-only diagnostic omitted the distro's `bwrap-userns-restrict` name, so its filename inventory alone cannot establish package absence.

Keep those failed artifacts with their original run and revision. A corrected prerequisite run must produce new evidence; it cannot replace the original failure.

## Selected distro payload

The inspected package is Ubuntu noble-updates `apparmor-profiles`, version **4.0.1really4.0.1-0ubuntu0.24.04.9**, architecture **all**:

| Input | Exact value |
| --- | --- |
| Package path | `pool/main/a/apparmor/apparmor-profiles_4.0.1really4.0.1-0ubuntu0.24.04.9_all.deb` |
| Package bytes | `39592` |
| Package SHA-256 | `90b02aa006eea7702cd4e851343e469e41365dda42145a3cb035de1d6c773b8c` |
| Extracted profile | `usr/share/apparmor/extra-profiles/bwrap-userns-restrict` |
| Profile SHA-256 | `11d39094f044f0cda0febb3ad517b830301da6b2ce929664af09ee9e4dd264f9` |
| Installed profile | `/etc/apparmor.d/bwrap-userns-restrict` |

The bytes and profile text were inspected from the [official Ubuntu archive package](https://archive.ubuntu.com/ubuntu/pool/main/a/apparmor/apparmor-profiles_4.0.1really4.0.1-0ubuntu0.24.04.9_all.deb). The [Ubuntu package record](https://packages.ubuntu.com/noble-updates/apparmor-profiles) identifies the package as experimental profiles; this is an explicit opt-in to a distro-provided policy, not a security certification. Ubuntu [moved the restrictive bwrap profile to the optional package after regressions](https://lists.ubuntu.com/archives/oracular-changes/2024-August/004905.html). Refuse unavailable versions, mismatching hashes and pre-existing policies rather than choose another profile automatically.

## Security boundary

The exact distro policy attaches `bwrap` to `/usr/bin/bwrap`. Its setup profile allows user namespaces, capability operations and mounts, plus broad existing file/network access; it does not itself provide the codec's resource or filesystem confinement. `allow px /** -> bwrap//&unpriv_bwrap` stacks the child policy at execution. The `unpriv_bwrap` child retains its transition through `allow pix /** -> &unpriv_bwrap` and **`audit deny capability,`**. The payload has neither an unconfined nor complain profile flag. Its optional local includes are refused if site overrides exist. These permissions allow setup inside the user namespace; the helper adds no host capabilities or file capabilities.

[Ubuntu documents per-application user-namespace permission through AppArmor](https://documentation.ubuntu.com/security/security-features/privilege-restriction/apparmor/). [Bubblewrap documents its use of `PR_SET_NO_NEW_PRIVS` and the importance of the supplied sandbox arguments](https://github.com/containers/bubblewrap/blob/main/README.md). The approved application contract remains `docs/04-spoolin-techdoc.md` around lines 5391 and 5531: non-root execution, read-only filesystem, no network or shell, restricted mounts and resource bounds. The production `CodecCommand.kt` still owns namespace isolation, mounts, fixed environment/executables, admission and resource limits. No global sysctl change, capability addition, seccomp relaxation or unrestricted codec fallback is introduced.

The inspected Noble profile does not contain the `mediate_deleted` flag found in some newer upstream revisions. This helper installs the inspected distro bytes unchanged. Different behavior or additional AppArmor denials must be diagnosed against actual remote execution rather than patched speculatively.

## Remote integration and evidence

Root owns CI and `prepare-codec-fixture.sh`. Invoke the new helper on the verified GitHub-hosted `ubuntu24` runner **after bubblewrap is installed and before the unchanged existing namespace probe**:

```bash
python3 ops/ci/test-codec-profile-package.py
# Inside prepare-codec-fixture.sh, immediately after its existing apt install:
bash ops/ci/prepare-codec-profile.sh
# Continue its unchanged production-stage preparation and namespace probe.
```

The helper needs `/usr/bin/bwrap`, `getcap`, the Ubuntu archive keyring and AppArmor parser already supplied by the runner. It creates a fresh isolated APT index directory with only the HTTPS Ubuntu noble-updates main source and the Ubuntu keyring; unsigned/insecure repositories and unauthenticated downloads are disabled. It checks the exact package record before downloading and checks package/profile hashes before installation. No package maintainer script or unrelated optional profile is executed. [APT documents version selection, authenticated downloads and index refresh](https://manpages.ubuntu.com/manpages/noble/man8/apt-get.8.html). [Ubuntu documents loading one profile with `apparmor_parser`](https://ubuntu.com/server/docs/how-to/security/apparmor/).

The helper refuses existing profile files, overrides, disabled entries or loaded bwrap profiles. It compares caller capabilities, seccomp, no-new-privileges and available user-namespace restriction settings before and after loading, and records the unchanged values. It loads only the selected policy in enforce mode. Retain `artifacts/source-checks/codec-profile-prerequisite.json` plus the actual helper log, existing before/after diagnostics, sandbox preflight log and codec test reports. The JSON explicitly leaves host and container probes pending. The Python tests exercise synthetic metadata/tampering faults only.

A missing authenticated package, unsupported runner, conflicting installed policy, denied profile load or unchanged-probe failure is a nonzero prerequisite failure. Investigate the actual retained error. Re-running the installer on the same runner deliberately refuses its existing policy; use the already-loaded policy for subsequent unchanged probes or use a fresh hosted job.

The actual production container still needs its own probe under read-only rootfs, all capabilities dropped, no-new-privileges and the configured Docker AppArmor/seccomp controls. A host profile pass cannot replace that result. Application startup, codec integration cases, browser artwork behavior and the final combined acceptance run remain independent obligations.
