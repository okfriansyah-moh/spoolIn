# PRD generation skills comparison

Research date: 9 October 2026, Asia/Jakarta. Scope: public GitHub projects, skill directories, first-party documentation, public PRD examples, and the two supplied Confluence pages. This is a sampled market comparison, not an exhaustive internet ranking. Findings only; no skill installation, PRD generation, issue publication, or SpoolIn implementation was performed.

The SpoolIn workspace was empty when inspected. There is no existing codebase, framework, or project PRD against which to validate integration. Fit below means fit for an upcoming PRD authoring workflow in Codex, not a tested integration or a feature added to SpoolIn.

GitHub stars and forks measure the entire repository. Last pushed is repository activity, not necessarily the date the PRD skill changed. Exact counts below come from GitHub's repository API; linked count cells are the metadata sources. Usability and adoption effort are source-based assessments, not generation benchmarks.

| Repository | Stars | Forks | Last pushed UTC | License |
|---|---:|---:|---|---|
| [mattpocock/skills](https://github.com/mattpocock/skills) | [281,614](https://api.github.com/repos/mattpocock/skills) | 23,604 | 2026-10-08 | MIT |
| [github/spec-kit](https://github.com/github/spec-kit) | [140,531](https://api.github.com/repos/github/spec-kit) | 12,582 | 2026-10-08 | MIT |
| [Fission-AI/OpenSpec](https://github.com/Fission-AI/OpenSpec) | [71,421](https://api.github.com/repos/Fission-AI/OpenSpec) | 4,882 | 2026-10-08 | MIT |
| [bmad-code-org/BMAD-METHOD](https://github.com/bmad-code-org/BMAD-METHOD) | [53,968](https://api.github.com/repos/bmad-code-org/BMAD-METHOD) | 6,075 | 2026-10-07 | MIT (LICENSE inspected) |
| [alirezarezvani/claude-skills](https://github.com/alirezarezvani/claude-skills) | [27,891](https://api.github.com/repos/alirezarezvani/claude-skills) | 3,915 | 2026-08-30 | MIT |
| [phuryn/pm-skills](https://github.com/phuryn/pm-skills) | [26,845](https://api.github.com/repos/phuryn/pm-skills) | 2,844 | 2026-09-14 | MIT |
| [snarktank/ralph](https://github.com/snarktank/ralph) | [21,935](https://api.github.com/repos/snarktank/ralph) | 2,097 | 2026-02-02 | MIT |
| [snarktank/ai-dev-tasks](https://github.com/snarktank/ai-dev-tasks) | [7,799](https://api.github.com/repos/snarktank/ai-dev-tasks) | 1,707 | 2025-11-05 | Apache-2.0 |
| [stellarlinkco/myclaude](https://github.com/stellarlinkco/myclaude) | [2,753](https://api.github.com/repos/stellarlinkco/myclaude) | 300 | 2026-05-04 | AGPL-3.0 |
| [product-on-purpose/pm-skills](https://github.com/product-on-purpose/pm-skills) | [715](https://api.github.com/repos/product-on-purpose/pm-skills) | 91 | 2026-10-08 | Apache-2.0 |
| [jamesrochabrun/skills](https://github.com/jamesrochabrun/skills) | [216](https://api.github.com/repos/jamesrochabrun/skills) | 26 | 2026-01-14 | MIT |

BMAD's API returned NOASSERTION, but its [actual LICENSE](https://github.com/bmad-code-org/BMAD-METHOD/blob/main/LICENSE) contains the MIT license plus a separate trademark notice. Counts are a snapshot; cached GitHub pages and skill directories can show different rounded totals.

The candidates have different scopes:

| Candidate and inspected source | Type | Observed authoring behavior | Adoption effort and limits for this project |
|---|---|---|---|
| [BMAD bmad-prd](https://github.com/bmad-code-org/BMAD-METHOD/blob/main/skills/bmad-prd/SKILL.md) | PRD skill within a full delivery framework | Create, update, validate; fast or coached discovery; stable requirement IDs, assumptions, review and decision history | Higher setup: BMad project configuration, shared scripts and uv. Upstream documents Codex plugins. Supports deep PRDs, but adds a broader workflow. |
| [Alireza product-manager-toolkit](https://github.com/alirezarezvani/claude-skills/blob/main/product-team/skills/product-manager-toolkit/SKILL.md) | PM toolkit with PRD templates | Standard, one-page, feature-brief and agile-epic formats; separate /prd command adds an evidence-focused interview | Low to medium for selected files. Codex support documented upstream; toolkit installation does not imply command parity. |
| [Phuryn create-prd](https://github.com/phuryn/pm-skills/blob/main/pm-execution/skills/create-prd/SKILL.md) | Dedicated PRD skill in a PM collection | Eight sections covering background, objectives, segments, value, solution and release; reads supplied context and flags assumptions | Low for the skill; native Codex installation documented. Base template needs added detail to match the supplied acceptance-criteria tables. |
| [Ralph prd](https://github.com/snarktank/ralph/blob/main/skills/prd/SKILL.md) | Dedicated PRD skill bundled with an autonomous coding workflow | 3–5 essential questions, story IDs, verifiable acceptance checklists, numbered requirements, scope and metrics | Low for PRD authoring alone. Hard-coded dev-browser and typecheck/lint assumptions need adaptation. Full runner and PRD authoring are separate. |
| [AI Dev Tasks create-prd](https://github.com/snarktank/ai-dev-tasks/blob/main/create-prd.md) | Reusable Markdown prompt | Clarification followed by overview, goals, stories, requirements, non-goals, design, constraints, metrics and questions | Very low to use as a prompt. It is not packaged as SKILL.md; automatic skill discovery needs adaptation. Per-story acceptance checklists are not explicitly required. |
| [Myclaude product-requirements](https://github.com/stellarlinkco/myclaude/blob/master/skills/product-requirements/SKILL.md) | Interactive PRD skill | Iterative discovery; acceptance criteria, flows, errors, nonfunctional constraints and scope; self-scored completion gate | Medium adaptation: hard-coded AskUserQuestion, persona and Chinese output. The 90/100 gate is the model's judgment, not an independently validated quality score. |
| [Product on Purpose deliver-prd](https://github.com/product-on-purpose/pm-skills/blob/main/skills/deliver-prd/SKILL.md) | Dedicated PRD skill in a PM collection | Baseline/target metrics, scope, testable requirements, dependencies and risks; conditional requirement verification and handoff contract | Low to medium; Codex support documented and reference files required. Assumes the product problem and solution direction are already aligned. |
| [James Rocha Brun prd-generator](https://github.com/jamesrochabrun/skills/blob/main/skills/prd-generator/SKILL.md) | Dedicated PRD skill with references and scripts | Discovery, 13-section PRD, user stories with acceptance criteria, metrics frameworks and review checklist | Low to medium; portable Markdown is plausible, but native Codex behavior was not tested. Smaller adoption signal; validator limitations below. |
| [Matt Pocock to-spec](https://github.com/mattpocock/skills/blob/main/skills/engineering/to-spec/SKILL.md) | Engineering specification synthesis skill | Synthesizes an existing discussion; user stories, implementation/testing decisions and exclusions; publishes to configured tracker | Medium contextual setup: requires prior discussion and tracker vocabulary. Does not conduct a discovery interview. Largest repository in this sample, but broader than PRD authoring. |
| [GitHub Spec Kit](https://github.com/github/spec-kit) | Specification-driven development toolkit | Constitution, specify, plan, tasks, implement and convergence; specification template has prioritized journeys, Given/When/Then, edge cases and measurable outcomes | Medium to high: Python 3.11+, uv and project initialization. [Codex integration](https://github.github.io/spec-kit/reference/integrations.html) is documented. Product/business context needs additional framing. |
| [OpenSpec](https://github.com/Fission-AI/OpenSpec) | Specification and change-management toolkit | Change proposals, requirements/scenarios, technical design, tasks, and archived changes updating living specifications | Medium: Node.js 20.19+ and project initialization. Codex supported. Main value is managing evolving requirements; conventional market/strategy PRD needs an additional template. |

Several distinctions affect usability:

- Phuryn's [write-prd command](https://github.com/phuryn/pm-skills/blob/main/pm-execution/commands/write-prd.md) adds explicit prioritized stories with acceptance criteria, non-goals, baseline/target/measurement metrics and owned questions. Those are not equivalent to the base create-prd skill's eight-section template. Its [Codex documentation](https://github.com/phuryn/pm-skills#codex-cli-openai) says Claude slash commands are not exposed as Codex slash commands.
- Alireza's separate [prd command](https://github.com/alirezarezvani/claude-skills/blob/main/commands/prd.md) asks five questions sequentially and gates drafting on problem evidence, target user and success metric. Its [code-to-prd](https://github.com/alirezarezvani/claude-skills/blob/main/product-team/code-to-prd/skills/code-to-prd/SKILL.md) derives an existing product baseline from code. An empty SpoolIn folder provides no baseline for that flow.
- Matt Pocock's old write-a-prd entry is [unavailable in the directory](https://www.skills.sh/mattpocock/skills/write-a-prd). The [current source](https://github.com/mattpocock/skills/blob/main/skills/engineering/to-spec/SKILL.md) is to-spec; the [changelog](https://github.com/mattpocock/skills/blob/main/CHANGELOG.md) records to-prd being renamed to to-spec. Old installation tutorials should not determine the current entry point.
- BMAD's [template](https://github.com/bmad-code-org/BMAD-METHOD/blob/main/skills/bmad-prd/assets/prd-template.md) provides optional sections for stakeholders, rollout, operational constraints, integrations and public API contracts. Technical implementation detail can move to an addendum, so its output would not automatically preserve your Confluence layout.
- James Rocha Brun's [validator source](https://github.com/jamesrochabrun/skills/blob/main/skills/prd-generator/scripts/validate_prd.sh) uses heading and text-pattern checks. It does not establish requirement correctness. Source inspection also found set -e combined with counters using post-increment from zero, which can terminate the script at the first increment; this is a static finding, not an execution test.
- Product on Purpose's [template](https://github.com/product-on-purpose/pm-skills/blob/main/skills/deliver-prd/references/TEMPLATE.md) has explicit FR IDs, edge-case behavior, and a conditional FR-to-verification map with verifiers. Its AI-behavior section applies when the product feature produces model output, not merely because AI authors the PRD. Detailed story acceptance criteria may live in linked story documents.

Skill-specific popularity provides another signal:

| Skill directory entry | Displayed installs | Observation |
|---|---:|---|
| [mattpocock/skills to-spec](https://www.skills.sh/mattpocock/skills/to-spec) | 631.3K | Engineering synthesis, not discovery-led PRD generation |
| [phuryn/pm-skills create-prd](https://www.skills.sh/phuryn/pm-skills/create-prd) | 10.4K | Dedicated PRD skill |
| [snarktank/ralph prd](https://www.skills.sh/snarktank/ralph/prd) | 1.4K | Dedicated PRD skill inside the Ralph repository |

These are platform-reported, rounded counts observed on 9 October 2026. They are not independently audited unique users, successful generations, or proof of output quality. The repository directory and individual skill pages can be cached at different times.

The supplied examples establish a useful comparison baseline:

| Reference | Observed content | Implication for evaluating a generator |
|---|---|---|
| [Formula Column Basic Math Functions](https://jurnal.atlassian.net/wiki/spaces/Officeless/pages/50994053137) — Confluence version 18, updated 17 June 2026 | Change log, objectives, expected outcomes, problem, scope, 21 numbered story sections, Given/When/Then acceptance tables, regression and edge cases, analytics and staged release | Check that output includes observable behavior and failure cases, not only a feature list. |
| [Formula Column Advanced Functions and Transformations](https://jurnal.atlassian.net/wiki/spaces/Officeless/pages/51200065784) — Confluence version 15 / document v1.1, updated 21 July 2026 | Ownership and approval metadata, detailed changes, data model, API/error contracts, flows, constraints, dependencies, decisions, questions, acceptance criteria and release gates | Check whether the skill can preserve this depth or deliberately distribute it across linked PRD, design and QA artifacts. |
| Attached social-media images | Overview, problem, goal, target users, core features and success metrics; separate PRD, design-system, architecture and agent-instruction documents | Useful introductory outline, but the visible PRD has less implementation and QA detail than the Confluence examples. |

The Confluence pages and images were treated as contextual examples, not instructions to install tools, run their workflows, or import their business requirements into SpoolIn. No internal page contents were submitted as search queries or posted to external repositories.

Source-based coverage comparison for the most directly relevant approaches:

| Approach | Product/business framing | Verifiable requirement detail | Match to supplied depth | Main gap |
|---|---|---|---|---|
| BMAD bmad-prd | Broad, with discovery modes | Stable FR IDs and testable consequences | High structural coverage with optional sections | Broader setup and different document organization |
| Phuryn create-prd | Strong explicit objectives, segments and value | Base skill less prescriptive; command adds AC tables | Partial from skill alone | Need to reconcile skill versus command and add QA/contract detail |
| Ralph prd | Goals, scope and success metrics | Explicit story acceptance checklists | Strong for individual feature behavior | Governance, release gates and detailed contracts need expansion |
| Product on Purpose deliver-prd | Problem, goals, baselines and targets | FR IDs, edge cases, conditional verification map | Substantial handoff coverage | Assumes discovery alignment; detailed ACs can be linked artifacts |
| Spec Kit specify | Feature-focused | Given/When/Then, priorities, edge cases, outcomes | Strong QA-oriented feature specification | Broader product strategy and Confluence document governance are outside the base template |

Coverage labels are judgments from the inspected source files. No candidate was tested on a shared SpoolIn brief, so there is no defensible measured quality ranking or time-to-first-PRD estimate.

Public examples and templates worth comparing:

| Source | What it is | Useful evidence | Limitation |
|---|---|---|---|
| [Atlassian product requirements template](https://www.atlassian.com/software/confluence/templates/product-requirements) | First-party Confluence template | Ownership, objectives, success metrics, assumptions, requirements, design references, open questions and exclusions | A template, not a completed production PRD |
| [Product on Purpose Recurring Tasks](https://github.com/product-on-purpose/pm-skills/blob/main/skills/deliver-prd/references/EXAMPLE.md) | Completed fictional worked PRD | Shows how scope, metrics, story priorities and detailed behavior fit together | Explicitly invented product, research and metrics; numbers must not be reused as evidence |
| [Aha PRD examples and templates](https://www.aha.io/roadmapping/guide/requirements-management/what-is-a-good-product-requirements-document-template) | First-party examples and agile/release/presentation templates | Different levels of detail and links between goals, requirements and delivery | Illustrative examples rather than independent evidence of product outcomes |
| [Intercom Intermission](https://www.intercom.com/blog/how-we-build-software) | First-party historical project-brief approach | Less-than-one-page problem, success measurement and scope, before solution design | A discovery brief rather than an implementation-ready PRD; the article is historical |

Additional sampled candidates were not promoted into the main comparison:

| Repository | Stars / forks | Reason for limited coverage |
|---|---:|---|
| [mohitagw15856/pm-claude-skills](https://github.com/mohitagw15856/pm-claude-skills) | [1,433 / 253](https://api.github.com/repos/mohitagw15856/pm-claude-skills) | Large general-purpose collection; metadata sampled, PRD workflow not deeply evaluated |
| [pmYangKun/create-prd-skill](https://github.com/pmYangKun/create-prd-skill) | [348 / 37](https://api.github.com/repos/pmYangKun/create-prd-skill) | Chinese-language B2B 14-section approach; recognized license not returned by API |
| [TestAny-io/testany-agent-skills](https://github.com/TestAny-io/testany-agent-skills) | [82 / 23](https://api.github.com/repos/TestAny-io/testany-agent-skills) | PRD writer with formal review and traceability; smaller adoption and more workflow dependencies |
| [johnnychauvet/prd-skill](https://github.com/johnnychauvet/prd-skill) | [7 / 3](https://api.github.com/repos/johnnychauvet/prd-skill) | Small adoption signal; recognized license not returned by API |
| [Liwei-Ji/SkillPRD](https://github.com/Liwei-Ji/SkillPRD) | [1 / 0](https://api.github.com/repos/Liwei-Ji/SkillPRD) | Small adoption signal; recognized license not returned by API |

A missing recognized license in repository metadata is not proof that no licensing statement exists. No license analysis or reuse decision was made.

Research method: searched web and GitHub using PRD, product requirements, SKILL.md and related specification-workflow terms; followed candidate claims to maintained source files, templates, README documentation and repository metadata; inspected skill-directory counters separately; compared against supplied reference structures. Catalogs and promotional claims were used for discovery, not accepted as proof of quality. No installation decision or integration plan has been selected.

