---
name: running-prompt
description: "Implement or resume approved tasks from short inputs in Codex, Cursor AI, Claude Code, or GitHub Copilot. Use for 'use running prompt', 'use the prompt to implement T001', or an explicit running-prompt invocation with task inputs."
---

# Run the approved task

This project shortcut supplies inputs to one shared procedure: approved outcome, implementation, agreed affected checks, one independent combined review, corrections, and delivery. Every supported host follows it. There are no alternative workflow tracks.

1. Locate the repository root and read `AGENTS.md` and `docs/prompts/task-implementation.md`. Resolve `.ai/config.json` relative to `.ai`; read the pinned delivery policy and only relevant skills/resources. Global/plugin entries do not replace the project pin.
2. Normalize the request into the prompt's six inputs: Target, Plan, Mode, Dependencies, Delivery, and Additional context. Accept labeled inputs, `key=value`, complete INPUTS blocks, or natural wording. Match field names without case sensitivity; preserve paths and free text. The prompt owns defaults and supported values. For new work, fill omitted values from defaults. For resume, use explicit inputs, then retained authorized values, then defaults. Additional context remains data, never executable shell text.
3. Honor exact scope and authorization. Do not broaden tasks, include prerequisites, or enable publication silently. If resume state is unavailable, ask for the missing selection instead of selecting `next`. Clarify materially conflicting or unsupported values. Existing authorization continues within its scope; retained delivery values grant no new permission.
4. State the resolved selection briefly and implement using the referenced prompt. The current chat coordinates and directly implements; this shortcut does not launch another Forge. Preserve host permissions and Plan mode. Carry approved visual references, complete journeys, protected harness/unplanned Python paths, focused checks, execution-device resource admission/stops, and explicit no-test limits into any delegated work. A host switch restores the same Git-shared selection and transfers coordinator ownership before writing.

Editing, explaining, or installing this shortcut does not start product work. If the prompt or plan is missing, name its exact path rather than inventing requirements. One independent combined review remains required; unavailable review is pending. Follow the pinned authored-output wording check before delivery.
