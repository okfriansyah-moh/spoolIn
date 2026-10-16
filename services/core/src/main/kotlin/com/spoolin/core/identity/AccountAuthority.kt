package com.spoolin.core.identity

import java.util.UUID

enum class AccountRole { VIEWER, MAINTAINER }

class AccountAuthority internal constructor(
    val id: UUID,
    val role: AccountRole,
    val authEpoch: Long,
)
