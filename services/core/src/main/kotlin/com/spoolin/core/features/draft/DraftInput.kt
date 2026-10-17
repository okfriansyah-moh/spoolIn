package com.spoolin.core.features.draft

import java.util.UUID

/**
 * Parsed wire input for [SaveDraft] / GraphQL DraftInput type.
 * Separates the top-level Core mutation identity (the operationId argument on saveDraft)
 * from [operationId], which is the *optional* original Catalog submission UUID stored on
 * the draft row for reconciliation before a Catalog commit.
 */
data class DraftInput(
    /** Allowlisted form target: MOVIE, PERSON or CREDIT. */
    val targetKind: String,
    /** Existing Catalog entity UUID; null for new-entity drafts. */
    val targetId: UUID?,
    /** Catalog entity version when the draft was started; null for new entities. */
    val baseVersion: Long?,
    /** Validated JSON object (UTF-8 encoded); max 65,536 bytes plaintext. */
    val values: String,
    /** Original Catalog submission UUID to reconcile before resuming; distinct from Core receipt ID. */
    val operationId: UUID?,
    /** SHA-256 policy revision hex string used when this draft was last saved. */
    val policyRevision: String,
)
