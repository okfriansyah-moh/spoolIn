package com.spoolin.core.recovery.crypto

import com.spoolin.core.DatabaseSettings
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.NonceAllocator
import tools.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path

object CrashAfterReservation {
    @JvmStatic
    fun main(arguments: Array<String>) {
        val config = ObjectMapper().readTree(Files.readAllBytes(Path.of(arguments[0])))
        val pool =
            DatabaseSettings(
                config.get("url").asString(),
                config.get("user").asString(),
                Path.of(config.get("passwordFile").asString()),
                false,
            ).pool()
        val nonce = NonceAllocator(pool).reserve("pii-write", KeyPurpose.PII)
        Files.writeString(
            Path.of(arguments[1]),
            java.util.HexFormat
                .of()
                .formatHex(nonce.bytes()),
        )
        if (arguments[2] == "crash") Runtime.getRuntime().halt(73)
        pool.close()
    }
}
