package com.spoolin.core.features.mcpgateway

import com.spoolin.core.features.connector.ConnectorRecord
import java.util.UUID

data class ConnectorContext(
    val connector: ConnectorRecord,
    val accountRole: String,
    val correlationId: UUID,
)
