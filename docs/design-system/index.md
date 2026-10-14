# SpoolIn design-system handoff

Start with [03-design-system.md](../03-design-system.md) and [PRD v2.4](../02-PRD.md) when creating the technical document. The PRD owns product requirements; the design specification and linked UI define their presentation and visible interaction contracts.

One system, with Cinema at night as the dark theme and Film gallery as the light theme. Both share components, layouts, and behavior.

[Brand history](brand-history.md) · [Branding verification](verification.md) · [Design specification](../03-design-system.md) · [UI gallery](mockup/index.html) · [Component catalog](mockup/components.html) · [Component contracts](components.md) · [Tokens JSON](tokens.json) · [Tokens CSS](mockup/tokens.css) · [PRD coverage](coverage.md) · [Coverage JSON](coverage.json) · [Theme preview](mockup/assets/design-preview.jpg) · [Artwork provenance](mockup/assets/artwork-provenance.json)

All views support both themes and 1440/390/320px responsive web contexts. Authentication, saves, uploads, and AI results use labeled fixtures. Validation findings and technical-design boundaries are consolidated in the specification.

## Folder layout

Markdown specifications, PRD coverage, contrast measurements, and `tokens.json` live in this directory. All rendered UI lives under `mockup/`: the gallery, component catalog, 12 flow pages, stylesheets (including the token CSS), scripts, and local assets. The CSS token representation continues to match the source JSON.

## Discovery

- [Discover movies and TV](mockup/discovery.html#catalog) — Reader.
- [Finding titles](mockup/discovery.html#catalog-loading) — Reader.
- [The catalog is empty](mockup/discovery.html#catalog-empty) — Reader.
- [The catalog could not load](mockup/discovery.html#catalog-failure) — Reader.
- [Results need refreshing](mockup/discovery.html#catalog-outdated) — Reader.
- [Search needs correction](mockup/discovery.html#catalog-invalid) — Reader.
- [No titles match](mockup/discovery.html#catalog-no-match) — Reader.

## Details

- [Movie details](mockup/details.html#movie) — Reader.
- [TV series details](mockup/details.html#tv) — Reader.
- [This title is unavailable](mockup/details.html#unavailable) — Reader.
- [Same title, different films](mockup/details.html#identities) — Reader.

## Account

- [Email and password login](mockup/account.html#login) — Visitor.
- [Create a Viewer account](mockup/account.html#register) — Visitor.
- [Your Viewer account is ready](mockup/account.html#registered) — Visitor.
- [Registration could not complete](mockup/account.html#registration-failure) — Visitor.
- [Signed in as Viewer](mockup/account.html#viewer) — Viewer.
- [Signed in as Maintainer](mockup/account.html#maintainer) — Maintainer.

## Session

- [Checking your session](mockup/session.html#session-check) — Visitor.
- [Sign-in is temporarily unavailable](mockup/session.html#auth-unavailable) — Visitor.
- [Your session expired](mockup/session.html#expired) — Visitor.
- [You do not have access](mockup/session.html#denied) — Viewer.
- [Check before continuing](mockup/session.html#recheck) — Maintainer.
- [This draft belongs to another account](mockup/session.html#other-account) — Viewer.
- [Sign out](mockup/session.html#signout) — Maintainer.
- [Signed out](mockup/session.html#signed-out) — Visitor.
- [Sign-out did not complete](mockup/session.html#signout-failed) — Maintainer.
- [Sign-out is not confirmed](mockup/session.html#signout-uncertain) — Visitor.

## Management

- [Manage movies](mockup/management.html#movies) — Maintainer.
- [Edit movie · retained stale draft](mockup/management.html#editor) — Maintainer.
- [Create movie](mockup/management.html#create) — Maintainer.
- [Movie saved](mockup/management.html#saved) — Maintainer.
- [Changes were not saved](mockup/management.html#save-failed) — Maintainer.
- [Save result not confirmed](mockup/management.html#save-uncertain) — Maintainer.
- [Original save confirmed](mockup/management.html#save-resolved) — Maintainer.
- [This movie is now archived](mockup/management.html#target-changed) — Maintainer.
- [Artwork is temporarily unavailable](mockup/management.html#media-outage) — Maintainer.
- [Leave this draft?](mockup/management.html#draft-cancel) — Maintainer.

## People

- [People](mockup/people.html#people) — Reader.
- [Loading people](mockup/people.html#people-loading) — Reader.
- [No people available](mockup/people.html#people-empty) — Reader.
- [People could not load](mockup/people.html#people-failure) — Reader.
- [This person is unavailable](mockup/people.html#person-unavailable) — Reader.
- [Person details](mockup/people.html#person) — Reader.
- [Create or edit an active person](mockup/people.html#person-edit) — Maintainer.
- [This person changed](mockup/people.html#person-conflict) — Maintainer.
- [Restore before editing](mockup/people.html#person-archived) — Maintainer.

## Credits

- [Manage movie credits](mockup/credits.html#credits) — Maintainer.
- [This credit already exists](mockup/credits.html#credit-duplicate) — Maintainer.
- [Retained credit · archived person](mockup/credits.html#credit-retained) — Maintainer.

## Artwork

- [Add or replace artwork](mockup/artwork.html#artwork) — Maintainer.
- [Transferring artwork](mockup/artwork.html#artwork-transfer) — Maintainer.
- [Transfer complete · processing](mockup/artwork.html#artwork-processing) — Maintainer.
- [Artwork attached](mockup/artwork.html#artwork-attached) — Maintainer.
- [Upload cancelled](mockup/artwork.html#artwork-cancelled) — Maintainer.
- [This file cannot be used](mockup/artwork.html#artwork-invalid) — Maintainer.
- [Artwork was not attached](mockup/artwork.html#artwork-failed) — Maintainer.
- [The movie or your access changed](mockup/artwork.html#artwork-target) — Maintainer.
- [Attachment is not confirmed](mockup/artwork.html#artwork-uncertain) — Maintainer.
- [Artwork rules changed](mockup/artwork.html#artwork-policy) — Maintainer.

## Archives

- [Movie archive](mockup/archives.html#movie-archive) — Maintainer.
- [Archive The Last Light](mockup/archives.html#movie-impact) — Maintainer.
- [The Last Light restored](mockup/archives.html#movie-restore) — Maintainer.
- [Permanently delete The Last Light](mockup/archives.html#movie-delete) — Maintainer.
- [Person archive](mockup/archives.html#person-archive) — Maintainer.
- [Archive Alex](mockup/archives.html#person-impact) — Maintainer.
- [Alex restored](mockup/archives.html#person-restore) — Maintainer.
- [Permanently delete Alex](mockup/archives.html#person-delete) — Maintainer.
- [The deletion impact changed](mockup/archives.html#impact-changed) — Maintainer.
- [Record permanently deleted](mockup/archives.html#deleted) — Maintainer.
- [Loading private archive](mockup/archives.html#archive-loading) — Maintainer.
- [No archived records](mockup/archives.html#archive-empty) — Maintainer.
- [Archive action failed](mockup/archives.html#archive-failed) — Maintainer.
- [Lifecycle result not confirmed](mockup/archives.html#archive-uncertain) — Maintainer.
- [Archive access denied](mockup/archives.html#archive-denied) — Viewer.
- [Confirm the updated deletion impact](mockup/archives.html#impact-current) — Maintainer.

## Audit

- [Catalog history](mockup/audit.html#audit) — Maintainer.
- [History entry](mockup/audit.html#audit-detail) — Maintainer.
- [Loading history](mockup/audit.html#audit-loading) — Maintainer.
- [No history yet](mockup/audit.html#audit-empty) — Maintainer.
- [History could not load](mockup/audit.html#audit-failed) — Maintainer.
- [History access denied](mockup/audit.html#audit-denied) — Viewer.

## Ai

- [Chatgpt proposal review](mockup/ai.html#chatgpt) — Maintainer.
- [Claude proposal review](mockup/ai.html#claude) — Maintainer.
- [Approval received; creation pending](mockup/ai.html#ai-approved) — Maintainer.
- [Movie creation confirmed](mockup/ai.html#ai-created) — Maintainer.
- [Review the revised proposal](mockup/ai.html#ai-corrected) — Maintainer.
- [Proposal rejected](mockup/ai.html#ai-rejected) — Maintainer.
- [Creation not authorized](mockup/ai.html#ai-denied) — Maintainer.
- [Required information needs correction](mockup/ai.html#ai-unknown) — Maintainer.
- [Creation result not confirmed](mockup/ai.html#ai-uncertain) — Maintainer.
- [Untrusted source instructions ignored](mockup/ai.html#ai-hostile) — Maintainer.
- [Revision 2 approved](mockup/ai.html#ai-approved-v2) — Maintainer.
- [Revision 2 creation confirmed](mockup/ai.html#ai-created-v2) — Maintainer.

## Policies

- [Text and Unicode boundaries](mockup/policies.html#text-boundaries) — Reader.
- [Clear versus leave unchanged](mockup/policies.html#optional-clearing) — Maintainer.
- [The input policy changed](mockup/policies.html#policy-changed) — Maintainer.
- [Editing is temporarily unavailable](mockup/policies.html#policy-unavailable) — Maintainer.
