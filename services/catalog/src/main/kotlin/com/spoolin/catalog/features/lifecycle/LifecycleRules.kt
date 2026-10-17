package com.spoolin.catalog.features.lifecycle

/** Archive-first lifecycle controls (AC-110 / AC-133). */
object LifecycleRules {
    enum class Action { ARCHIVE, RESTORE, DELETE }

    fun allowedActions(status: String): Set<Action> =
        when (status) {
            "ACTIVE" -> setOf(Action.ARCHIVE)
            "ARCHIVED" -> setOf(Action.RESTORE, Action.DELETE)
            else -> emptySet()
        }

    fun requiresConfirmation(action: Action): Boolean = action == Action.DELETE

    const val CANCEL_CHANGES_NOTHING: Boolean = true
}
