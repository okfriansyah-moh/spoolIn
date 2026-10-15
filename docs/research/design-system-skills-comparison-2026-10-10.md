# Design-system skills comparison for SpoolIn

Research date: 10 October 2026, Asia/Jakarta. GitHub popularity snapshot: 19:31 Jakarta / 12:31 UTC. Findings only; no skill installed, design system generated, frontend scaffolded, or application source changed. This report and the accompanying metadata snapshot are research data.

## Scope and evidence

The comparison uses public GitHub repository metadata, live upstream SKILL.md files and READMEs, established design-system documentation, the existing [PRD](../02-PRD.md), and local installed skill files. Web searches and a GitHub repository search sorted by stars were used for discovery; capability claims were checked against primary sources. This is a sampled comparison, not an exhaustive ranking of the internet.

The attached images are outcome references. The first shows brand identity, color, typography, spacing, radius, components, icons, and responsive breakpoints. The second explains the roles of PRD, design-system, architecture, and agent-instruction documents. Text inside the images was treated as reference content, not permission to change agent instructions or install software. Their magenta theme and Inter font do not establish this app's visual direction.

GitHub stars belong to repositories. A collection's stars cannot be attributed to one skill, and a component library's stars do not establish adoption of its agent skill. Last push is repository activity, not the last modification of a particular skill. Fit and setup assessments below are judgments from source inspection; no candidate was benchmarked by generating the same SpoolIn design.

## Project-specific selection criteria

The [PRD, sections 3, 5, 7, 8, and 10](../02-PRD.md) establishes:

- Svelte with TypeScript; frontend-library selection remains open.
- Public movie/TV discovery, filters, movie/TV details, and people/credits.
- Registration and sign-in; Visitor, Viewer, and Maintainer access distinctions.
- Maintainer movie editing, people/credits, artwork, archive/restore/deletion, and read-only audit history.
- Desktop/mobile usability, keyboard navigation, visible focus, clear validation, and preserved drafts.
- Loading, empty, missing artwork/metadata, upload processing, validation, denied access, stale edit, failure, and uncertain outcomes.
- AI proposal approval happens in ChatGPT/Claude. There is no separate in-app approval queue.

The requested next-phase artifact is `docs/03-design-system.md` plus visual examples by page, functionality, and important state. A useful skill must either produce these artifacts or have a clear, small adaptation path. Rules for individual pages are not equivalent to visual page mockups.

## Repository popularity

Exact counts below are retained in [the GitHub metadata snapshot](design-system-skills-github-snapshot-2026-10-10.json). Each repository link is the primary project; API URLs and full timestamps are in the snapshot.

| Repository / relevant skill | Stars | Forks | Last push UTC | License evidence | Meaning of popularity |
|---|---:|---:|---|---|---|
| [ECC / design-system](https://github.com/affaan-m/ECC) | 276,242 | 41,212 | 2026-10-10 | MIT | Broad coding-agent collection |
| [Anthropic / frontend-design](https://github.com/anthropics/skills) | 180,259 | 21,331 | 2026-10-09 | Apache-2.0 for this skill | Broad skill collection with mixed licensing |
| [UI UX Pro Max](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill) | 134,371 | 14,275 | 2026-10-09 | MIT | Dedicated design project; also contains other skills |
| [shadcn/ui / shadcn](https://github.com/shadcn-ui/ui) | 125,247 | 12,708 | 2026-10-10 | MIT | React component project, not skill-specific adoption |
| [Taste Skill](https://github.com/Leonxlnx/taste-skill) | 94,281 | 6,416 | 2026-10-09 | MIT | Design/implementation/image skill family |
| [Impeccable](https://github.com/pbakaus/impeccable) | 79,237 | 4,713 | 2026-10-09 | Apache-2.0 | Dedicated design tooling project |
| [Vercel / web-design-guidelines](https://github.com/vercel-labs/agent-skills) | 32,157 | 2,819 | 2026-08-28 | MIT stated by README; API field absent | Broad skill collection |
| [Anthropic knowledge-work / design-system](https://github.com/anthropics/knowledge-work-plugins) | 28,584 | 3,267 | 2026-10-10 | Apache-2.0 | Broad plugin collection |
| [Alireza / ui-design-system](https://github.com/alirezarezvani/claude-skills) | 27,949 | 3,923 | 2026-08-30 | MIT | Broad skill collection |
| [ibelick / ui-skills](https://github.com/ibelick/ui-skills) | 9,576 | 443 | 2026-10-10 | MIT | UI authoring/review skill collection |
| [shadcn-svelte / shadcn-svelte](https://github.com/huntabyte/shadcn-svelte) | 9,181 | 575 | 2026-10-05 | MIT | Svelte component project, not skill-specific adoption |
| [Google Stitch skills](https://github.com/google-labs-code/stitch-skills) | 8,464 | 1,108 | 2026-08-17 | Apache-2.0 | Stitch skill/plugin collection |
| [Hue](https://github.com/dominikmartn/hue) | 838 | 66 | 2026-06-11 | MIT | Design-language skill generator |
| [Work with design systems](https://github.com/natdexterra/work-with-design-systems) | 52 | 4 | 2026-10-02 | MIT | Focused Figma design-system skill |
| [Signer Labs design-system builder](https://github.com/signerlabs/design-system-builder-skills) | 7 | 1 | 2026-03-20 | MIT | Focused design-system generation skill |

Anthropic's repository API does not report one license for its entire skills collection; the [frontend-design LICENSE.txt](https://github.com/anthropics/skills/blob/main/skills/frontend-design/LICENSE.txt) establishes that skill's Apache-2.0 terms. The installed ClaudeKit-authored `design-system` skill also appears in UI UX Pro Max's repository; its stars must not be counted as another independent adoption signal.

## Most relevant candidates and actual outputs

| Candidate and inspected source | What it produces | Fit for this project | Setup / adaptation |
|---|---|---|---|
| [UI UX Pro Max](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill/blob/main/.claude/skills/ui-ux-pro-max/SKILL.md) | Style, palette, type, spacing, UX and stack recommendations; ASCII/Markdown/JSON. Upstream persistence writes `design-system/<project-slug>/MASTER.md` and `pages/<page>.md` overrides. | High as the initial recommendation engine; Svelte and HTML supported. Page files contain rules, not rendered mockups. | Already installed locally. Python search/generator; Node needed only for CLI installation. Normalize selected rules into the requested document and author component/state/page coverage. |
| [Installed design-system / ClaudeKit](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill/blob/main/.claude/skills/design-system/SKILL.md) | Primitive → semantic → component token structure, JSON starter, CSS generation, token validation, component state/spec patterns. | High for token architecture and documented rules. No complete PRD-to-page-gallery contract. | Already installed locally; Node scripts and reference files. Select its app-design sections; its large slide/presentation branch is unrelated to SpoolIn. |
| [Impeccable](https://github.com/pbakaus/impeccable/blob/main/.agents/skills/impeccable/SKILL.md) | UI craft, critique/audit/harden/adapt/polish workflows; [document](https://github.com/pbakaus/impeccable/blob/main/.agents/skills/impeccable/reference/document.md) writes root `DESIGN.md`. Existing-code scan adds `.impeccable/design.json` with tokens/component HTML/CSS specimens. Seed mode omits real extracted tokens and sidecar. | High as a visual design and quality companion; framework-neutral guidance fits Svelte. Existing-code documentation is stronger than preimplementation token extraction. | Codex explicitly supported. Current launcher uses a bundled/downloadable binary; installer also adds edit hooks. Requested folder and complete page gallery require explicit project integration. |
| [Google Stitch skills](https://github.com/google-labs-code/stitch-skills) | [generate-design](https://github.com/google-labs-code/stitch-skills/blob/main/plugins/stitch-design/skills/generate-design/SKILL.md) generates screens/variants with HTML and screenshots; [design-md](https://github.com/google-labs-code/stitch-skills/blob/main/plugins/stitch-utilities/skills/design-md/SKILL.md) documents Stitch screens; manage-design-system applies a shared theme. | High for visually exploring screens; medium for the exact local deliverable because generation is service-backed and exports need organizing. | Codex supported; generation/theme management requires Stitch MCP and credentials. Defaults use `.stitch/`. React build skills do not implement this project's Svelte requirement. |
| [shadcn-svelte skill](https://github.com/huntabyte/shadcn-svelte/blob/main/skills/shadcn-svelte/SKILL.md) | Svelte component composition, semantic theme tokens, presets, project/component context, documented UI patterns. | High as an optional implementation foundation after design choices. It does not prescribe a PRD-derived Markdown specification or static page gallery. | Node/package manager, shadcn-svelte CLI and its styling setup. Adopting this library is a separate choice from choosing a generator. [README](https://github.com/huntabyte/shadcn-svelte) identifies it as the community-led Svelte port of shadcn/ui. |
| [Signer Labs builder](https://github.com/signerlabs/design-system-builder-skills/blob/main/design-system-builder/SKILL.md) | Explicit `design-tokens.json`, `design-tokens.css`, `design-system.md`, `design-system-preview.html`; HTML includes swatches, type/spacing specimens, components and a mini-app. Inputs: style description, image, or Figma. | Very high format match, weak popularity evidence. The specimen/mini-app is not a complete page set. | Portable Markdown instructions; Figma MCP only for Figma input. Redirect paths and add PRD pages/states. Its hard font bans and screenshot-derived exact-value claims need contextual review. |
| [Hue](https://github.com/dominikmartn/hue/blob/main/SKILL.md) | Generates a reusable design-language skill, `design-model.yaml`, token/component/platform docs, and `preview.html`, `component-library.html`, `landing-page.html`, `app-screen.html`. | High for an integrated token/document/visual package; more elaborate than the requested project documentation. | Codex supported. Defaults target a generated skill folder; user paths supported. Requires brand/reference discovery and supporting templates. Prescribed marketing/dashboard content must yield to the PRD's catalog scope. |
| [Anthropic frontend-design](https://github.com/anthropics/skills/blob/main/skills/frontend-design/SKILL.md) | Compact token/design planning, ASCII layouts, frontend code, visual critique. | High for authoring distinctive HTML/Svelte prototypes from an approved system; medium as the sole design-system generator. | Lightweight instruction skill; upstream documents Claude, so Codex use is adaptation. No durable Markdown destination or automatic page-gallery contract. It overlaps with Impeccable's design guidance. |

### Other candidates and when they become useful

| Candidate and inspected source | Concrete capability | Why it is secondary here |
|---|---|---|
| [ECC design-system](https://github.com/affaan-m/ECC/blob/main/skills/design-system/SKILL.md) | Existing CSS/code scan, competitor research, `DESIGN.md`, `design-tokens.json`, self-contained `design-preview.html`, and visual audits. | Good document/specimen match, but its generation sequence starts with existing UI. Largest repository in the sample is a broad collection; it does not prove this short skill is the best generator. |
| [Anthropic knowledge-work design-system](https://github.com/anthropics/knowledge-work-plugins/blob/main/design/skills/design-system/SKILL.md) | Audit/document/extend templates: tokens, props, variants, states, accessibility, behavior and usage. | Strong documentation completeness reference once direction is chosen. No prescribed files or HTML/image generation. Cowork connector placeholders need adaptation for Codex. |
| [Alireza ui-design-system](https://github.com/alirezarezvani/claude-skills/blob/main/product-team/skills/ui-design-system/SKILL.md) | Python token generator; JSON/CSS/SCSS/summary exports; component documentation, responsive calculations and handoff guidance. | Useful deterministic token scaffolding; limited visual/page outputs. Already-installed token tooling covers much of the same role. |
| [ibelick create-design-md](https://github.com/ibelick/ui-skills/blob/main/skills/create-design-md/SKILL.md) | Reconstructs `DESIGN.md` from an existing product repo or rendered website, with evidence and schema-validation gates. [fixing-accessibility](https://github.com/ibelick/ui-skills/blob/main/skills/fixing-accessibility/SKILL.md) covers keyboard/focus/contrast/form errors. | Good for extracting an actual reference or documenting implemented UI. Requires real design evidence and produces no new page mockups. |
| [Taste Skill](https://github.com/Leonxlnx/taste-skill) | Layout/type/motion/density guidance, implementation skills, Codex-oriented variant, and separate image-only reference-board skills. | Strong popularity and useful creative exploration; no project design-system document contract. More overlapping art-direction guidance is not necessarily useful beside Impeccable/frontend-design. |
| [Official React shadcn skill](https://github.com/shadcn-ui/ui/blob/main/skills/shadcn/SKILL.md) | Consumes existing `DESIGN.md`, themes a React app; [showcase](https://github.com/shadcn-ui/ui/blob/main/skills/shadcn/design-system-page.md) includes tokens, contrast, component state matrices, recipes and a realistic screen. | Excellent specimen-page reference, but its React/TSX workflow conflicts with the required Svelte frontend. |
| [Work with design systems](https://github.com/natdexterra/work-with-design-systems) | Figma inspect/build workflows; component/state/a11y audits, handoff docs; optional token CSS and code-audit export. | Relevant if Figma becomes the source of truth. Requires Figma MCP and appropriate access; adds a tool dependency to a currently local Markdown/HTML objective. The older generate-design-system URL redirects here. |
| [Vercel web-design-guidelines](https://github.com/vercel-labs/agent-skills/blob/main/skills/web-design-guidelines/SKILL.md) | Reads current interface guidelines and reports source-file violations. | Optional final review layer; it is not a design-system or mockup generator. |

## Existing local capabilities

Read-only inspection confirmed `/Users/mekari/.codex/skills/ui-ux-pro-max/SKILL.md` and `/Users/mekari/.codex/skills/design-system/SKILL.md` plus their data/scripts/references. They provide an existing recommendation and token-specification foundation without installing a new package. Upstream and installed instructions differ in some details; installation presence is not proof that the local copy matches current upstream.

The local UI UX Pro Max instructions describe `design-system/MASTER.md` and `design-system/pages/`; current upstream documents a project-slug subfolder. Neither default is the requested `docs/03-design-system.md`. The future workflow must deliberately reconcile paths and keep one authoritative set of design rules.

## Established design-system examples

These are reference systems, not agent skills. Reuse their documentation/interaction ideas selectively; no visual identity or library is adopted by this report.

| Primary example | What is useful for the requested outcome | SpoolIn fit |
|---|---|---|
| [Singapore Government Design System token architecture](https://www.designsystem.tech.gov.sg/foundations/token-architecture) and [home](https://www.designsystem.tech.gov.sg/) | Clearly separates primitive, semantic and component tokens; organized foundations and accessible/mobile service components. | Strong structural example for design-token organization. The PRD does not require SGDS styling. |
| [Carbon data-table guidelines](https://www.carbondesignsystem.com/building-blocks/core/components/data-table/guidelines) | Live examples, anatomy, sizes, variants, interaction and usage guidance alongside components. | Good reference for Maintainer archive/audit tables and management views. Do not import unsupported batch actions merely because the example supports them. |
| [Atlassian token foundations](https://atlassian.design/foundations/tokens) | Semantic token library, design/code usage guidance, migration and theme consistency. | Useful for meaningful token names and consistent shared UI across public and Maintainer surfaces. |
| [Material 3 color roles](https://m3.material.io/styles/color/the-color-system) and [type roles](https://m3.material.io/styles/typography/applying-type) | Documents color by purpose and typography by display/headline/title/body/label roles. | Useful foundation model for catalog hierarchy and readable mobile controls; no Material theme adoption implied. |
| [GOV.UK error summary](https://design-system.service.gov.uk/components/error-summary/) and [error messages](https://design-system.service.gov.uk/components/error-message/) | Linked field corrections, focus handling, specific validation messages and distinction between input errors and service problems. | Strong behavioral example for registration/editor forms and the PRD's recoverable feedback rules. |
| [shadcn-svelte](https://www.shadcn-svelte.com/) and [React showcase guidance](https://github.com/shadcn-ui/ui/blob/main/skills/shadcn/design-system-page.md) | Live component examples and a model for displaying swatches, typography, spacing, state matrices and real screens. | Closest optional component-family fit to the required frontend; the showcase's concepts can inform a framework-independent HTML catalog. |

## Possible combinations for the next decision

These are alternatives for discussion, not selected installations or an approved implementation plan.

| Combination | Division of responsibilities | Principal tradeoff |
|---|---|---|
| Existing UI UX Pro Max + existing design-system + Impeccable | Recommendation engine → token/component specification → screen authoring and design review. A small project contract supplies file paths and PRD coverage. | Strong popularity/role fit with substantial local foundation. Need to assess Impeccable's package footprint and path assumptions. |
| Existing UI UX Pro Max + existing design-system + frontend-design | Same foundations; lightweight frontend skill authors HTML/Svelte page examples. Use documentation templates for completeness. | Simpler tool setup, but less structured persistent documentation/review machinery. |
| Existing foundations + Google Stitch generation/theme skills | Foundations define the shared language; Stitch explores screens and exports HTML/screenshots for organization under docs. | Direct visual artifacts; requires an external generation service and MCP setup. Svelte implementation is separate. |
| Hue as the package generator + selected review guidance | One shared model feeds a generated skill, documentation, component catalog and representative screens. | Strong bundled visuals but broader scope, smaller adoption signal, and more adaptation away from brand/marketing defaults. |
| Signer Labs builder + frontend-design or Impeccable | Builder creates the four baseline artifacts; frontend guidance extends them into actual PRD page/state examples. | Closest baseline filename/output match; only 7 stars and source-based usability evidence. |

Use one visual-authoring lead, one token authority, and optional review guidance. Loading several overlapping art-direction skills does not supply missing PRD coverage and can introduce conflicting font, layout, or motion prescriptions. This is a source-based integration assessment, not measured proof that any combination generates better designs.

## What the requested deliverable would need to cover

This inventory defines how to judge the candidates. No files in `docs/design-system/` have been created during this research pass.

| Proposed artifact / visual group | Evidence it should show | PRD anchor |
|---|---|---|
| `03-design-system.md` | Direction/rationale, tokens, color/contrast, type, layout/spacing/radius, icons/artwork, component variants/states, responsive/focus/motion rules, links to visuals | NFR-007; sections 5.5 and 8.10 |
| Token JSON/CSS plus component catalog HTML | Shared values, button/input/filter/card/dialog/feedback specimens and focus/disabled/loading/error states | AC-083 through AC-085; AC-139 |
| Discovery/search/filter mocks | Movie/TV distinction, genre/year/month behavior, unknown dates, no results and loading/failure | US-001, US-002; section 8.2 |
| Movie/TV detail and person mocks | Metadata, artwork fallback, credits and unknown optional information; no TV-management controls | US-001, US-005; sections 3.1 and 5.1 |
| Registration/sign-in mocks | Field bounds/corrections, pending/denied outcomes and Viewer registration | US-008; section 8.3 |
| Maintainer movie editor mocks | Create/edit, validation, preserved draft, stale conflict and uncertain save | US-003, US-018; section 8.4 |
| People/credits management mocks | Person reuse, role/character entry, lifecycle impact preview including TV credits | US-004; section 8.6 |
| Artwork mocks | Selection/transfer/processing/attachment, failed replacement retaining the current image | US-007, US-011; section 8.7 |
| Archive/restore/delete mocks | Separate movie/person archives, current impact, irreversible deletion confirmation | Sections 5.2, 8.5 and 8.6 |
| Audit mocks | Read-only list/detail, actor/action/outcome, approval versus creation, empty/loading/failure/denied states | US-009; section 8.9 |
| Chat-flow visual or annotated storyboard | Proposal → human review/correction/rejection → exact-content approval → confirmed/uncertain creation, inside ChatGPT/Claude context | US-019; section 8.8 |

Responsive examples should cover desktop and mobile for the main journeys; a state can share a page or scenario instead of requiring a separate file. HTML is useful for inspectable layout, responsive behavior and interactions; images support side-by-side review but do not prove keyboard behavior or token consistency. A style-guide specimen alone is insufficient to explain this product's flows.

Streaming/player, watchlist, ratings/reviews, TV editing, role-management settings, an operations console, and an in-app AI approval queue must not appear as shipped capabilities: they are excluded or deferred by the PRD. The final document's colors, fonts, layout and library remain undecided.
