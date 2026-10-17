package com.spoolin.media.features.hold

/** Protective reference states for cross-title artwork retention (§6.2.6 / ATDD-249). */
enum class ReferenceState {
    RESERVED,
    ACTIVE,
    RELEASED,
    ;

    fun canTransitionTo(to: ReferenceState): Boolean = to in ALLOWED[this].orEmpty()

    companion object {
        private val ALLOWED: Map<ReferenceState, Set<ReferenceState>> = mapOf(
            RESERVED to setOf(ACTIVE, RELEASED),
            ACTIVE to setOf(RELEASED),
            RELEASED to emptySet(),
        )
    }
}
