package com.spoolin.core.features.crypto

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class HashingBusy : IllegalStateException("Credential verification is busy")

class PasswordHasher(
    private val engine: PasswordEngine,
    private val policy: NewPasswordPolicy = NewPasswordPolicy(),
) : AutoCloseable {
    private val executor =
        ThreadPoolExecutor(
            2,
            2,
            0,
            TimeUnit.MILLISECONDS,
            ArrayBlockingQueue(2),
            { runnable -> Thread(runnable, "account-password-hash").also { it.isDaemon = true } },
            ThreadPoolExecutor.AbortPolicy(),
        )

    fun hashNew(password: String): StoredPasswordHash {
        require(policy.accepts(password)) { "New password is outside policy" }
        return execute(password) { engine.hash(it) }
    }

    fun verify(
        password: String,
        stored: StoredPasswordHash,
    ): PasswordVerification = execute(password) { engine.verify(it, stored) }

    private fun <T> execute(
        password: String,
        operation: (ByteArray) -> T,
    ): T {
        check(
            !org.springframework.transaction.support.TransactionSynchronizationManager
                .isActualTransactionActive(),
        ) {
            "Hashing requires an independent transaction boundary"
        }
        val bytes = PasswordInput.exactBytes(password)
        val future =
            try {
                executor.submit(
                    Callable {
                        try {
                            operation(bytes)
                        } finally {
                            bytes.fill(0)
                        }
                    },
                )
            } catch (_: RejectedExecutionException) {
                bytes.fill(0)
                throw HashingBusy()
            }
        return try {
            await(future)
        } finally {
            if (future.isCancelled) bytes.fill(0)
        }
    }

    private fun <T> await(future: java.util.concurrent.Future<T>): T =
        try {
            future.get()
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            throw HashingBusy()
        } catch (_: ExecutionException) {
            throw ProtectionUnavailable()
        }

    override fun close() {
        executor.shutdown()
        if (!executor.awaitTermination(HASH_SHUTDOWN_SECONDS, TimeUnit.SECONDS)) {
            executor.shutdownNow().filterIsInstance<java.util.concurrent.Future<*>>().forEach { it.cancel(false) }
        }
    }
}

internal const val HASH_SHUTDOWN_SECONDS = 30L
