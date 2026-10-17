package com.spoolin.core.features.mcpgateway

object ConnectorRpcContext {
    val tokenHash: ThreadLocal<ByteArray?> = ThreadLocal.withInitial { null }
}
