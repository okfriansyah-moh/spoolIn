package com.spoolin.support.identity

import com.google.protobuf.Message

internal object ActorMessages {
    fun check(
        value: Any,
        claims: ActorClaims,
    ) {
        val message = value as? Message ?: throw ActorRejected()
        val header = message.descriptorForType.findFieldByName("header")
        val payload = if (header != null && message.hasField(header)) message.getField(header) as Message else message
        checkContext(payload, claims)
        val actor = payload.descriptorForType.findFieldByName("actor_id")
        if (actor != null &&
            (!actor.hasPresence() || payload.hasField(actor))
        ) {
            require(payload.getField(actor) == claims.accountId.toString())
        }
        if (claims.method.endsWith("/GetOperation")) {
            val id = requireNotNull(payload.descriptorForType.findFieldByName("id"))
            require(payload.getField(id) == claims.operationId?.toString())
        }
    }

    private fun checkContext(
        message: Message,
        claims: ActorClaims,
    ) {
        val field = message.descriptorForType.findFieldByName("context") ?: return
        require(message.hasField(field))
        val context = message.getField(field) as Message
        val operation = requireNotNull(context.descriptorForType.findFieldByName("operation_id"))
        val correlation = requireNotNull(context.descriptorForType.findFieldByName("correlation_id"))
        require(context.getField(operation) == claims.operationId?.toString())
        require(context.getField(correlation) == claims.correlationId.toString())
    }
}
