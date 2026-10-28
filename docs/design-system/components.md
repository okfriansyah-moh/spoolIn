# Shared component contracts

[Open the visual component catalog](mockup/components.html). Every specimen supports the same dark/light switch; there is no duplicate light component set. All consume [tokens.json](tokens.json) through [tokens.css](mockup/tokens.css).

| Component / anatomy | Variants and behavior | States / accessibility | Visual |
|---|---|---|---|
| Wordmark and frame mark | Full wordmark; 16/24/48px compact icon | Decorative frame hidden when adjacent name labels it | [Identity](mockup/components.html#identity) |
| Button/link | Primary, secondary, danger; action versus navigation | Default, hover, visible focus, selected, disabled, pending; 44px target and explicit reason for disabled | [Buttons](mockup/components.html#buttons) |
| Field | Label, input/textarea, helper, error | Required/optional, invalid, disabled; associated help/errors; no password recovery persistence | [Fields](mockup/components.html#fields) |
| Filters | Title type, multi-genre, year/month, search, six-choice sort | Native checked state, OR genres and AND categories; clear text preserves others; month depends on year | [Filters](mockup/components.html#filters) |
| Title card | Artwork/fallback, full title, type, date precision, metadata | Long/missing/unknown; active record identity independent of text | [Artwork](mockup/components.html#artwork) |
| Navigation / metadata | Role-aware links, breadcrumb, badge, definition list, provenance disclosure | Visible current context; unknown distinct from zero; no direct private archive disclosure | [Navigation](mockup/components.html#navigation) |
| Notice / error summary | Status title, explanation, safe next action; field-linked summary | Pending/confirmed/failed/uncertain/denied; persistent, live when changed, no color-only state | [Feedback](mockup/components.html#feedback) |
| Progress / skeleton | Current criterion or operation context, measured progress, separate processing | No 100%-equals-success; quiet reduced-motion behavior | [Progress](mockup/components.html#progress) |
| Data list / table | Headings, stable record ID/context, actions appropriate to role | Wrap long values; no identity collapse; no arbitrary history restore | [History](mockup/audit.html#audit) |
| Confirmation dialog | Current title/impact/consequences, cancel, explicit final action | Focus starts at cancel; Tab contained; Escape cancels; opener receives focus | [Dialog](mockup/components.html#dialog) |
| Conflict review | Current saved version, retained draft, intentional review, save | Stale/removed/denied/uncertain; no force overwrite or duplicate submission | [Editor](mockup/management.html#editor) |
| Impact preview | Target plus active/archived movie and TV effects | Changed impact invalidates prior confirmation; archive before delete; restore before editing | [Updated impact](mockup/archives.html#impact-current) |
| Session boundary | Identity context, permitted intended destination, protected draft lock | Checking/expired/unavailable/denied; failed/uncertain sign-out; cross-account isolation | [Session flows](mockup/session.html#expired) · [Draft fixture harness](mockup/components.html#draft-isolation) |
| Proposal storyboard | Exact publishable values, source/unknowns, human correction/rejection/approval | Revision-specific approval; denial/uncertain outcome; no autonomous creation | [ChatGPT](mockup/ai.html#chatgpt) · [Claude](mockup/ai.html#claude) |
| Layout primitives | Container, stack/grid, form groups, spacing, art radius | Shared in both themes; 1440/390/320 review contexts | [Spacing](mockup/components.html#spacing) |

Use semantic color roles and component aliases. Theme overrides must never change action meanings or control order. Svelte responsibilities and optional library mappings live in [the design specification](../03-design-system.md#sveltetypescript-handoff). No framework or library is installed by these examples.


## Public title page controls

Use Previous titles page, Page N and Next titles page below the catalog grid. Unavailable directions are disabled; pending navigation disables both and exposes the requested direction as busy. Successful navigation replaces the grid and focuses the results heading; failures retain rows with directional retry and restart. Keep controls wrapping at narrow widths. Sort by uses the existing labeled select and resets pagination while retaining other criteria. The [catalog refinement](../03-design-system.md#public-catalog-sorting-and-navigation-refinement) owns labels and layout; [technical behavior](../04-spoolin-techdoc.md#public-catalog-sorting-and-page-navigation) owns state and contracts.
