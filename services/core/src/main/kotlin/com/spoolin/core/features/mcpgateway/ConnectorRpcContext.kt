package com.spoolin.core.features.mcpgateway

import io.grpc.Context

object ConnectorRpcContext {
    private val tokenHash: Context.Key<ByteArray> = Context.key("spoolin-connector-token-hash")

    fun current(): ByteArray? = tokenHash.get()

    fun context(hash: ByteArray): Context = Context.current().withValue(tokenHash, hash)
}
