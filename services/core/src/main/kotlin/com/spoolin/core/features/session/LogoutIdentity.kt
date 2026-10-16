package com.spoolin.core.features.session

import java.util.UUID

class LogoutIdentity(
    val sessionId: UUID,
    val accountId: UUID,
)
