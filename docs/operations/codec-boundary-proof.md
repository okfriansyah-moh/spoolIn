# Codec prerequisite and production-container proof

The codec has two separate execution boundaries. `ops/ci/prepare-codec-fixture.sh` builds the checksum-verified libvips 8.18.7 stage and runs the fixed header helper under bubblewrap as the GitHub runner user. This prepares real owner integration tests on that host. A host pass does not prove the Media container can create the same sandbox.

The host script now records read-only context before and after the actual helper attempt. `codec-host-before.json` and `codec-host-after.json` contain UID/GID, selected effective/bounding capabilities, no-new-privileges/seccomp state, namespace inode IDs and selected user-namespace sysctl values. Vendor bubblewrap profiles contribute presence/hash metadata only; loaded matching profiles include their observed mode. Missing or unreadable information stays unavailable.

Kernel diagnostics read at most 120 recent kernel messages into a bounded 128 KiB capture with a five-second deadline. Reports retain only finite operation/profile/capability categories and counts from AppArmor DENIED messages. Raw kernel logs, process IDs, paths, message payloads and unrelated profiles are discarded. Absence of a reported denial does not prove that a control allowed the operation. Probe stderr retention is capped at 4096 bytes, with observed length and truncation recorded separately.

The retained failure in run 39090751636 occurred at the host boundary on assessed SHA `97bd5270acdb68c47e4769e01862810bb841af90`, Ubuntu image `20261012.334.1`, bubblewrap `0.9.0-1ubuntu0.3`: `loopback: Failed RTM_NEWADDR: Operation not permitted`. Upstream [bubblewrap 0.9.0 network setup](https://github.com/containers/bubblewrap/blob/v0.9.0/network.c#L151-L170) identifies the failing loopback-address step. It occurs before the codec helper. This message alone does not identify the responsible security control, including because the netlink reply error path does not always set errno before printing it.

## Actual Media image probe

After actual `make start` readiness has passed, run sequentially in the same verified GitHub-hosted Linux environment:

```sh
python3 ops/ci/codec-diagnostics-test.py
python3 ops/ci/probe-media-codec.py
```

The first command checks diagnostic redaction, bounded capture, timeout handling and rejection of unsafe/mismatched container settings. It is not application proof.

The second requires the successful make-start readiness report and matching startup Git SHA. It creates uniquely named fixture files through running Media as UID/GID 10001 in the existing disposable CI Media volume. Directory/file modes are checked as 0700/0600. The input is the existing public `letters.png` design fixture; its copied checksum is checked.

The probe creates a Media Compose one-off using the actual production image, service configuration and volume. No capabilities or security options are added. Before opening its execution gate, it inspects the effective container configuration and requires it to match running Media: UID10001, read-only root filesystem, all capabilities dropped, no additional capabilities, no-new-privileges, matching AppArmor profile, private network and actual shared Media volume. It records the effective process capabilities, no-new-privileges/seccomp state and namespace inode IDs. The helper does not print container environment values or secret mount source paths.

The gated process runs the fixed header and JPEG/PNG/WebP encoder commands with the production mount/environment/resource restrictions. A ten-second outer deadline bounds this small diagnostic pipeline. Each codec invocation uses the production address-space/CPU/file-size/open-file limits, network namespace isolation, restricted read-only mounts and bounded temporary filesystem. An additional bubblewrap info FD records created namespace IDs without changing those restrictions. Required mount/network/PID namespace IDs must be present and differ from the enclosing container. Upstream info-FD output omits the user namespace inode; the report states this observation limit explicitly.

Accepted outputs require real re-decoding with matching dimensions, supported magic, correct ownership, nonzero bounded size and measured SHA-256. Results retain the actual image ID, observed versions, SHA/run/runner metadata, stage outcomes, namespace observations and bounded codec diagnostics under `artifacts/codec-container/<unique-id>/summary.json`. The one-off container and its own fixture directory are removed after evidence collection, including failure paths. No database rows or UI state are modified.

A missing prerequisite, nonzero codec exit, timeout, unsafe configuration or missing namespace evidence fails this probe. Neither boundary changes AppArmor, seccomp, global user-namespace settings or capabilities; neither runs bubblewrap as root or under strace. A denied platform prerequisite remains an unresolved control/access obligation until observed evidence supports a compliant correction.

This positive diagnostic pipeline does not replace actual ProcessorRunner/worker integration, resource-kill/bomb/animation cases, storage/attachment authorization, browser observations or artwork acceptance. Those tests must run at their assigned boundaries against the corrected source and actual environment. Keep host and container results separate, preserve failed reports and do not infer a production pass from host preparation.
