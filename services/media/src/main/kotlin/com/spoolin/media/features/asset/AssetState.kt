package com.spoolin.media.features.asset

/**
 * Media asset lifecycle states (TD-ROW-L5019, §6.2.6).
 *
 * RECEIVING  → PROCESSING: byte stream accepted, job committed.
 * RECEIVING  → REJECTED:   stream failed / bounds exceeded / cancelled.
 * PROCESSING → READY:      immutable output persisted.
 * PROCESSING → REJECTED:   validation / resource-limit failure.
 * READY      → DELETING:   cleanup locked.
 * DELETING   → DELETED:    bytes unlinked and metadata tombstoned.
 */
enum class AssetState {
    RECEIVING,
    PROCESSING,
    READY,
    REJECTED,
    DELETING,
    DELETED,
    ;

    /** Returns true if [to] is a valid direct transition from this state. */
    fun canTransitionTo(to: AssetState): Boolean = to in ALLOWED[this].orEmpty()

    companion object {
        private val ALLOWED: Map<AssetState, Set<AssetState>> =
            mapOf(
                RECEIVING to setOf(PROCESSING, REJECTED),
                PROCESSING to setOf(READY, REJECTED),
                READY to setOf(DELETING),
                REJECTED to emptySet(),
                DELETING to setOf(DELETED),
                DELETED to emptySet(),
            )
    }
}
