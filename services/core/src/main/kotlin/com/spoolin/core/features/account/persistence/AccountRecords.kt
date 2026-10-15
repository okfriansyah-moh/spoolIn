package com.spoolin.core.features.account.persistence

import com.fasterxml.jackson.annotation.JsonIgnore
import com.spoolin.core.features.crypto.StoredPasswordHash
import java.util.UUID

enum class AccountRole { VIEWER, MAINTAINER }

data class AccountProjection(
    val id: UUID,
    val role: AccountRole,
    val version: Long,
)

class AccountCredential(
    val id: UUID,
    @get:JsonIgnore val password: StoredPasswordHash,
    val authEpoch: Long,
)

sealed interface AccountInsert {
    class Stored(
        val account: AccountProjection,
    ) : AccountInsert

    data object Conflict : AccountInsert

    data object Unavailable : AccountInsert
}
