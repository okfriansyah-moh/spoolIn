# MCP host provenance assessment

Assessment date: 2026-10-18. Assessed application source: `cb55ab2deb903e6c1cde85dc8ccc80f0d43e5cc7`. Scope: T028 and the trusted review flow under G-03. This is a source and primary-document capability assessment, not runtime evidence, an independent acceptance review or a security-contract change. The coordinator retains finding IDs and acceptance decisions in the shared ledger.

Publication remains disabled. The signer and Core verifier exist, but the production session does not invoke the signer or expose approval/rejection tools. No authenticated host-to-adapter UI-origin mechanism was identified in the inspected SDK APIs or the official documents below. These are unresolved implementation and host-capability obligations; this assessment does not resolve the source gap by documenting it.

## Approved requirements

Line references below identify the files at the assessed application revision.

| Approved source | Required boundary |
| --- | --- |
| `docs/04-spoolin-techdoc.md:5664–5668` | Exact-content human review, one bound human/client credential and verified client-host enforcement; transport membership alone does not establish actor identity. Unsupported human restriction or UI-origin protection disables publication. |
| `docs/04-spoolin-techdoc.md:5873–5914` | Exact proposal/revision/hash/challenge input; strict 32-byte challenge encoding. Catalog retains the digest; raw challenge travels only in private UI metadata, outside model content and logs. Corrections invalidate the prior revision authority. |
| `docs/04-spoolin-techdoc.md:5916–5918` | App-only approve/reject controls plus a separate adapter check of trusted host capability/channel provenance. Catalog checks live authorization, binding, expiry and one-use challenge. Forged labels and model calls cannot authorize publication. |
| `docs/04-spoolin-techdoc.md:5922–5930` | Registered private ChatGPT tunnel and distinct connector credential; actual Claude Desktop registration through the supported local transport. Registration and permission cannot be inferred from adapter readiness. |
| `docs/04-spoolin-techdoc.md:5938–5983` | Human UI invocation precedes adapter assertion and Core/Catalog verification; unsupported isolation selects the disabled-publication branch. |
| `docs/04-spoolin-techdoc.md:6041` | Publication defaults false. Enabling still rejects unsupported/unauthenticated channels. Separate MAC key; exact scoped claims, maximum 30-second expiry, maximum 4 KiB private RPC metadata, 60-second old-key read overlap. |
| `docs/05-atdd.md:7423–7457` — ATDD-186 | Independent installed-client evidence for isolation and authentic UI origin, including direct model and forged-label attempts. Adapter fixtures do not substitute. |
| `docs/05-atdd.md:7461–7495` — ATDD-187 | Real connector/client/epoch, MAC, invocation, method, audience, expiry and rotation negative paths across MCP, Core and Catalog. |
| `docs/05-atdd.md:8711–8745` — ATDD-221 | Actual Claude Desktop registration/version/OS, exact human approval, correction/rejection, response-loss recovery and adversarial attempts. |
| `docs/05-atdd.md:8749–8783` — ATDD-222 | Separate actual ChatGPT registration/version/OS and private tunnel proof with the same positive and negative boundaries. |

The adapter assertion is evidence issued after trusted-host authentication. It cannot itself authenticate an ordinary incoming tool call by being signed in that call's handler.

## Inspected SDK and primary documents

Installed package metadata is `node_modules/@modelcontextprotocol/ext-apps/package.json`, version **2.0.3**, matching the pinned application dependency. These installed artifacts were read without running package code:

- `dist/src/app.d.ts:639–673`: `App.callServerTool` accepts ordinary `CallToolRequest["params"]` and proxies through the host.
- `dist/src/app-bridge.d.ts:660–680`: the host-side handler receives ordinary tool-call parameters and request context. Its forwarding example sends the same `tools/call` parameters to the server. No signed UI-origin evidence is specified there.
- `dist/src/spec.types.d.ts`: host context/capabilities describe UI presentation, sandboxing and proxy support. These APIs do not define a server-verifiable human-origin credential.

Primary references inspected on the assessment date:

| Primary reference | Supported capability and limit |
| --- | --- |
| [Stable MCP Apps specification, 2026-01-26](https://github.com/modelcontextprotocol/ext-apps/blob/main/specification/2026-01-26/apps.mdx), Resource Discovery/Visibility and Communication Protocol | Host filters model tool discovery, restricts app-only calls to the same server and proxies iframe messages. This establishes obligations for a compliant host. The inspected specification does not supply a signed UI-origin attestation or independently authenticated adapter channel. |
| [MCP Apps SDK source](https://github.com/modelcontextprotocol/ext-apps) and the installed 2.0.3 declarations above | App/host proxy APIs are available. Host-side bridge context is not an authenticated proof delivered to this application's server. A host implementation could enforce a boundary internally; an installed host must demonstrate its actual mechanism and enforcement. |
| [Official OpenAI reference](https://developers.openai.com/plugins/reference), tool metadata/results and client-provided metadata | Private result metadata is delivered to the component outside the conversation transcript. Optional session/subject identifiers support correlation/identification; the documented user-agent hint is unsuitable for authorization. The inspected reference does not define a server-verifiable UI-origin attestation. |
| [Official Secure MCP Tunnels guide](https://developers.openai.com/api/docs/guides/secure-mcp-tunnels), referenced by approved TD §9.1 | Private transport registration is a separate prerequisite. Tunnel access does not by itself prove that a particular invocation originated in a human review component. This assessment does not claim a registered tunnel or authenticated host account. |

These are scoped observations about the inspected interfaces, not a claim that every current or future host lacks a usable mechanism. No external source text is reproduced here.

## Current production wiring

| Actual source | Disposition |
| --- | --- |
| `mcp/src/gateway/session.ts:140–170` | Creates the live gateway-backed session and registers resources, context, operation lookup and proposal. The session's gateway interface excludes approve/reject; there is no trusted review invocation handler or signer call site. |
| `mcp/src/gateway/session.ts:263–315` and `mcp/src/transport/propose-tool-result.ts` | Proposal processing obtains the owner's transient challenge and builds the separated result envelope. Existing projection code is source evidence only; installed-host secrecy still requires actual proof. |
| `mcp/src/identity/ui-assertion.ts:94–185` | Protected-key signer provides scoped assertion creation and rotation. Its production session invocation remains missing. Environment enablement is not proof of an authenticated host channel. |
| `mcp/src/operations/gateway-client.ts:107–120` | Real approve/reject RPC methods accept a private assertion. They are not invoked by the production session. Their presence alone does not complete the flow. |
| `mcp/src/review/main.ts:36–41` | Explicitly disables review buttons and displays the unavailable status. It does not receive authenticated host approval authority or call approve/reject. |
| `services/core/src/main/kotlin/com/spoolin/core/features/mcpgateway/McpGatewayRpc.kt:134–158` | Both RPC and blocking approval paths require the verifier before forwarding to Catalog. Connector bearer/challenge alone cannot pass this boundary. |
| `services/core/src/main/kotlin/com/spoolin/core/boundary/http/McpReviewApprovalGateway.kt` | HTTP route carries bearer/challenge but does not create trusted assertion context. The downstream verifier therefore remains required; this is not an alternate trusted host path. |
| `services/core/src/main/kotlin/com/spoolin/core/boundary/http/McpReviewUiGateway.kt` | HTTP challenge redemption is closed. Reopening it or putting a bearer in a URL would violate the approved boundary. |

No concrete displayed-review lifecycle defect is established by this assessment beyond the known unavailable trusted approval flow. No speculative UI features were added.

## Required completion evidence and unresolved mechanism

A supported host mechanism must authenticate UI-origin invocation to the adapter while binding the existing credential, client, live session and exact proposal revision/hash. It must distinguish a legitimate human review action from a model directly calling the same tool or copying a purported origin label. The precise mechanism, credentials and verification API are not defined by the inspected host interfaces. An invented header, model-supplied flag, user-agent check, arbitrary parent-window message or challenge-only assumption cannot silently replace the approved independent provenance check.

Once a supported mechanism is identified, production work remains: authenticated handler registration; private challenge delivery and lifecycle; signer invocation using authoritative bindings; actual RPC approval/rejection; cancellation/expiry/revision replacement/replay behavior; and sanitized result handling. Existing Core/Catalog controls must remain effective throughout. Secrets and redeemable assertions must remain outside model content, URLs, telemetry and captured UI.

Actual installed-client proof remains separately pending: each host's version/OS/protocol and private registration, model/UI separation, human-origin enforcement, current Maintainer binding, successful exact-content publication, rejection without publication, correction requiring fresh approval, credential/session/revision/hash/expiry/replay/race negatives, and original-operation lookup after response loss. A controlled SDK harness can test application behavior but cannot satisfy ATDD-221 or ATDD-222.

No application build, Make target, container, browser, client, integration suite or acceptance operation ran for this document. No host registration, runtime success or source completeness is claimed. The required external dependency is a documented, accessible and demonstrably enforced host provenance mechanism for each required client; publication stays disabled until it is supported and verified.
