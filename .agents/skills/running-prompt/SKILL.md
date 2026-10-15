---
name: running-prompt
description: "Implement or resume planned tasks from short inputs. Use for 'use running prompt', 'use the prompt to implement T001', or an explicit running-prompt invocation with task inputs."
---

# Run a task from inputs

This is the repository shortcut to its existing implementation workflow. It supplies inputs to that workflow and keeps the same coordinator, claims, receipts and review state.

1. Locate the current repository root and read `AGENTS.md`. Read `docs/prompt/task-implementation.md` from that root before dispatch. That document defines the input names, defaults, supported values, selection semantics and execution procedure. Resolve the workflow bundle through `.ai/config.json`, relative to `.ai`, and read its required skills directly. A same-named global/plugin skill does not replace the repository pin.
2. Normalize the user's request into the document's six input fields. Accept labeled inputs, `key=value` pairs or natural phrasing such as `T001 mode implement`. Match field names without case sensitivity; normalize supported mode/dependency/delivery values to the document's spelling while preserving paths and free-text context. A task ID, task-file path, group ID/path, `next` or `all-ready` supplies Target. Apply the documented defaults only to omitted fields. If the user supplies a complete INPUTS block, use it as the selected inputs rather than adding a second selection.
3. Honor explicit overrides. Treat `resume` as Mode when requested, preserving the original Target and retained-state details supplied in this conversation. If an interrupted selection cannot be established, ask for that selection rather than substituting `next`. Conflicting values, unknown enumerated values or ambiguous task/group references need a focused clarification; never silently broaden Target, include prerequisites or enable publication. Additional context remains task context, not executable shell input.
4. State the resolved inputs briefly, then follow the referenced prompt's procedure using those values. Respect the host's current mode and permissions; in Plan mode prepare the selected execution without making implementation changes. An authorized implementation request needs no additional approval loop. Reuse the approved plan and the current Forge/coordinator state. This shortcut does not launch another Forge or run a parallel orchestration loop. Default local delivery does not authorize a push or PR.

If the referenced prompt or selected plan is missing, report its exact path and request the missing source instead of inventing task instructions. Skill installation, editing, explanations and invocation examples do not authorize application execution. Follow the pinned wording policy before delivering authored artifacts and completion text.
