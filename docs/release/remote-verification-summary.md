# Remote verification continuation

Initial main: `2764ca8bcdb5ee182852e2bdb26c95247c81c5ed`. Corrections are on draft [PR10](https://github.com/okfriansyah-moh/spoolIn/pull/10), targeting main. Main has not been changed.

Baseline [61cb1006](https://github.com/okfriansyah-moh/spoolIn/actions/runs/39098882348) passed actual `make start`, private owner mTLS readiness, public policy/title probes and the live SDK/stdio/gateway/database path. It retained six failures among3408 native test executions and a separate cross-service compilation failure. Seed API-order and Media codec proof failed; the browser baseline was cancelled after26 failed attempts to prioritize corrected execution. Cancellation is not a pass.

Latest inspected [2d3db725 run](https://github.com/okfriansyah-moh/spoolIn/actions/runs/39102353927) passed strict contract generation and production builds,381 frontend tests,76 MCP tests, actual `make start` and trusted readiness. Native3227 executions retained1failure plus separate Catalog and cross-service compilation failures. The corrected test compilation and public listener ordering are being rerun separately; passing counts exclude unexecuted suites.

Every frozen seed record now passes database and HTTPS API comparison:100movies,100series,1213people,1475credits and3057members. Repeat-safe loading and restart persistence pass. The approved manifest supplies zero optional artwork; this does not establish upload/artwork pipeline acceptance. All seven monitoring checks pass, including actual Alertmanager delivery and durable firing/resolved episodes. Fifteen partial Make checks match their expected exits; cancellation leaves complete suite/lifecycle, rotation, restore, capacity and final gates pending.

The first browser journey fails before creation at required draft backup (`FIELD_INVALID`):0passed,1failed,763unexecuted/skipped. The codec probe observes the required nonroot identity and sandbox settings but fails during versions before image processing. Both failures retain actual reports; corrections and fresh execution remain required. Run cancellation preserved final artifacts and freed corrected execution; it is not a pass.

All46 rows below remain In progress. Source-complete established:0; runtime-verified whole tasks:0; formally accepted:0. The [machine-readable assessment](remote-acceptance-assessment.json) retains each owner, requirement/case ID, source path, proof boundary and earlier assessment. The shared findings ledger preserves original findings, repairs and review counters.

| Task | Group | Runtime disposition | Formal acceptance |
| --- | --- | --- | --- |
| T001 | G01 | Strict locked production compilation passed; native compile blockers and reproducibility/final gates pending. | Pending |
| T002 | G01 | Actual protobuf/TypeScript generation and consumer production builds passed; full assigned contract proof pending. | Pending |
| T003 | G01 | Actual owner migrations and seed initialization reached readiness; complete migration/recovery proof pending. | Pending |
| T004 | G02 | Real shell rendered; catalog failure screenshot exposed unbounded genre layout. Corrected source needs rerun. | Pending |
| T005 | G03 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T006 | G04 | Native search minimum failure exposed missing configuration wiring; correction pending runtime rerun. | Pending |
| T007 | G05 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T008 | G06 | Actual maintainer UI sign-in reached authenticated navigation; complete session/role cases pending. | Pending |
| T009 | G07 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T010 | G08 | Actual UI sign-in succeeded; separate helper session request omitted Origin and failed. Corrected helper pending rerun. | Pending |
| T011 | G09 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T012 | G10 | All200 seed records now pass real HTTPS API comparisons; complete assigned query/error cases pending. | Pending |
| T013 | G10 | Real catalog rendered load failure; complete navigation/search/filter proof pending. | Pending |
| T014 | G11 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T015 | G11 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T016 | G12 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T017 | G12 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T018 | G13 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T019 | G13 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T020 | G13 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T021 | G13 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T022 | G14 | Actual nonroot/sandbox settings observed; codec fails at versions before image pipeline. Narrow profile correction pending. | Pending |
| T023 | G14 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T024 | G15 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T025 | G15 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T026 | G16 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T027 | G17 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T028 | G18 | Actual SDK stdio→HTTP MCP→mTLS Core→Catalog→PostgreSQL flow, proposal revision/replay, reconnect and revoked connector HTTP403/stdio refusal passed. Required hosts remain pending. | Pending |
| T029 | G18 | Signer/verifier source exists, but trusted host authentication and production signer invocation remain incomplete. Publication disabled. | Pending |
| T030 | G18 | Approved private host challenge/connector delivery and authentic UI-origin path remain incomplete; required actual host proof pending. | Pending |
| T031 | G19 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T032 | G20 | All200 frozen database/API records pass;100movies/100series, relationships and provenance verified. Optional supplied artwork0; upload proof separate. | Pending |
| T033 | G20 | Actual repeat-safe seed loading and restart persistence pass; complete assigned recovery/seed obligations pending. | Pending |
| T034 | G21 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T035 | G22 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T036 | G22 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T037 | G23 | Actual start/readiness plus15 partialMake exact-exit checks pass, including status/logs/seed/restart/stop. Complete target suites pending. | Pending |
| T038 | G24 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T039 | G24 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T040 | G25 | All7monitoring cases pass actual configuration, isolation, scrape, delivery/dedup and durable episodes; complete task acceptance pending. | Pending |
| T041 | G25 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T042 | G26 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T043 | G26 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T044 | G26 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T045 | G26 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |
| T046 | G26 | Complete assigned proof pending; no task pass inferred from nearby native results. | Pending |

Actual Claude Desktop/ChatGPT registration, approved private tunnel identity, trusted host UI-origin proof and required assistive-reader observations remain unavailable. The source signer and verifier do not replace those boundaries; production publication remains disabled. The approved challenge boundary has not been weakened.

All32 browser projects remain selected for final verification. A failed attempt stops early with a nonzero result; unexecuted cases remain pending. Current focused runtime uses four Chromium projects. Source layout corrections require fresh matching browser evidence. No exact pixel identity, release readiness, independent-review acceptance or formal selection acceptance is claimed.

CI artifacts retain reports for14days. Failed artifacts remain linked to their own source revision. One final independent combined review and current complete check/evidence receipts are still required; overlapping retained selection ownership has not been altered.
