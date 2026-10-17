package com.spoolin.catalog.features.artwork

enum class ArtworkAttachmentState {
    PREPARING,
    ATTACHED,
    ABORTED,
    ;

    fun canTransitionTo(to: ArtworkAttachmentState): Boolean = to in ALLOWED[this].orEmpty()

    companion object {
        private val ALLOWED: Map<ArtworkAttachmentState, Set<ArtworkAttachmentState>> = mapOf(
            PREPARING to setOf(ATTACHED, ABORTED),
            ATTACHED to emptySet(),
            ABORTED to emptySet(),
        )
    }
}
