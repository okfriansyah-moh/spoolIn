package com.spoolin.core.recovery.crypto

import com.spoolin.core.features.crypto.Argon2Engine
import com.spoolin.core.features.crypto.HashingBusy
import com.spoolin.core.features.crypto.NewPasswordPolicy
import com.spoolin.core.features.crypto.PasswordEngine
import com.spoolin.core.features.crypto.PasswordHasher
import com.spoolin.core.features.crypto.PasswordInput
import com.spoolin.core.features.crypto.PasswordVerification
import com.spoolin.core.features.crypto.PhcParameters
import com.spoolin.core.features.crypto.StoredPasswordHash
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class CredentialIntegrationTest {
    @Test
    fun actualArgon2UsesTheSelectedPhcAndExactInputAcrossPolicyTightening() {
        val password = "a".repeat(14) + "é"
        val engine = Argon2Engine()
        PasswordHasher(engine).use { hasher ->
            val first = hasher.hashNew(password)
            val second = hasher.hashNew(password)
            assertNotEquals(first.encoded, second.encoded)
            val parameters = requireNotNull(PhcParameters.parse(first.encoded))
            assertEquals(65536, parameters.memory)
            assertEquals(3, parameters.iterations)
            assertEquals(1, parameters.parallelism)
            assertEquals(16, parameters.salt.size)
            assertEquals(32, parameters.output.size)
            assertTrue(hasher.verify(password, first).matched)
            assertFalse(hasher.verify(" $password", first).matched)
            assertFalse(hasher.verify("a".repeat(14) + "e\u0301", first).matched)
            assertNull(hasher.verify(password, first).replacement)
            PasswordHasher(engine, NewPasswordPolicy(20, 128)).use { tightened ->
                assertThrows(IllegalArgumentException::class.java) { tightened.hashNew(password) }
                assertTrue(tightened.verify(password, first).matched)
            }
        }
    }

    @Test
    fun successfulLegacyParametersOfferRehashButWrongCredentialNeverDoes() {
        val password = PasswordInput.exactBytes("existing exact credential")
        val old = legacy(password)
        val engine = Argon2Engine()
        val valid = engine.verify(password, old)
        assertTrue(valid.matched)
        assertNotNull(valid.replacement)
        assertFalse(requireNotNull(PhcParameters.parse(requireNotNull(valid.replacement).encoded)).needsRehash())
        assertTrue(engine.verify(password, requireNotNull(valid.replacement)).matched)
        val wrong = engine.verify(PasswordInput.exactBytes("wrong credential"), old)
        assertFalse(wrong.matched)
        assertNull(wrong.replacement)
        assertFalse(engine.verify(password, StoredPasswordHash("malformed")).matched)
        password.fill(0)
    }

    @Test
    fun realArgon2WorkHasTwoSlotsAndBoundedQueueAdmission() {
        val release = CountDownLatch(1)
        val start = CyclicBarrier(5)
        val engine = ObservedArgon2(release)
        val executor = Executors.newFixedThreadPool(5)
        PasswordHasher(engine).use { hasher ->
            try {
                val completion = ExecutorCompletionService<Boolean>(executor)
                repeat(5) {
                    completion.submit {
                        start.await(10, TimeUnit.SECONDS)
                        try {
                            hasher.hashNew("synthetic exact password")
                            true
                        } catch (_: HashingBusy) {
                            false
                        }
                    }
                }
                assertFalse(requireNotNull(completion.poll(10, TimeUnit.SECONDS)).get())
                assertTrue(engine.entered.await(10, TimeUnit.SECONDS))
                assertEquals(2, engine.maximum.get())
                release.countDown()
                repeat(4) { assertTrue(requireNotNull(completion.poll(30, TimeUnit.SECONDS)).get()) }
                assertEquals(4, engine.completed.get())
                assertEquals(2, engine.maximum.get())
            } finally {
                release.countDown()
                executor.shutdownNow()
            }
        }
    }

    @Test
    fun hashingCannotEnterAnActualDatabaseTransaction() =
        CryptoFixture().use { fixture ->
            PasswordHasher(Argon2Engine()).use { hasher ->
                TransactionTemplate(DataSourceTransactionManager(fixture.pool)).execute {
                    assertThrows(IllegalStateException::class.java) { hasher.hashNew("synthetic exact password") }
                }
                assertNotNull(hasher.hashNew("synthetic exact password"))
            }
            fixture.observe(
                "t005a3",
                "hashing-boundary",
                mapOf(
                    "insideTransactionRejected" to true,
                    "outsideTransactionSucceeded" to true,
                    "maximumConcurrentHashes" to 2,
                ),
            )
        }

    private fun legacy(password: ByteArray): StoredPasswordHash {
        val salt = "0123456789abcdef".toByteArray()
        val parameters =
            Argon2Parameters
                .Builder(Argon2Parameters.ARGON2_id)
                .withVersion(19)
                .withMemoryAsKB(32768)
                .withIterations(2)
                .withParallelism(1)
                .withSalt(salt)
                .build()
        val output = ByteArray(32)
        Argon2BytesGenerator().also { it.init(parameters) }.generateBytes(password, output)
        val base64 = Base64.getEncoder().withoutPadding()
        return StoredPasswordHash(
            "\$argon2id\$v=19\$m=32768,t=2,p=1\$${base64.encodeToString(salt)}\$${base64.encodeToString(output)}",
        )
    }
}

private class ObservedArgon2(
    private val release: CountDownLatch,
) : PasswordEngine {
    private val active = AtomicInteger()
    val maximum = AtomicInteger()
    val completed = AtomicInteger()
    val entered = CountDownLatch(2)
    private val actual = Argon2Engine()

    override fun hash(bytes: ByteArray): StoredPasswordHash {
        val running = active.incrementAndGet()
        maximum.accumulateAndGet(running, ::maxOf)
        entered.countDown()
        check(release.await(15, TimeUnit.SECONDS))
        return try {
            actual.hash(bytes).also { completed.incrementAndGet() }
        } finally {
            active.decrementAndGet()
        }
    }

    override fun verify(
        bytes: ByteArray,
        stored: StoredPasswordHash,
    ): PasswordVerification = actual.verify(bytes, stored)
}
