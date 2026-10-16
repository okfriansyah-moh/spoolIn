package com.spoolin.support.identity

import java.util.UUID

object Permissions {
    const val PUBLIC_READ: Boolean = true

    fun management(role: String?): Boolean = role == "MAINTAINER"

    fun owned(
        role: String?,
        actor: UUID,
        owner: UUID,
    ): Boolean = role in setOf("VIEWER", "MAINTAINER") && actor == owner

    const val TELEVISION_WRITE: Boolean = false
}
