package com.spoolin.core.recovery.crypto

import com.spoolin.core.features.account.persistence.AccountIdentity
import com.spoolin.core.features.account.persistence.AccountInsert
import com.spoolin.core.features.account.persistence.AccountPayload
import com.spoolin.core.features.account.persistence.AccountRole
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.account.persistence.ProtectedAccounts
import com.spoolin.core.features.crypto.AccountCryptography
import com.spoolin.core.features.crypto.AeadCipher
import com.spoolin.core.features.crypto.Argon2Engine
import com.spoolin.core.features.crypto.KeyRegistry
import com.spoolin.core.features.crypto.NonceAllocator
import com.spoolin.core.features.crypto.PasswordInput
import com.spoolin.core.features.crypto.ProtectedRead
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class IdentityPoolIntegrationTest {
    @Test
    fun eightIdentityReadsCompleteWithOneConnection() = concurrentReads(1)

    @Test
    fun eightIdentityReadsCompleteWithTheRuntimePoolLimit() = concurrentReads(8)

    private fun concurrentReads(maximum: Int) =
        CryptoFixture().use { fixture ->
            val identity = IdentityRules().identity("Synthetic pool read", "pool-read@example.org")
            val id = UUID.randomUUID()
            val hash = Argon2Engine().hash(PasswordInput.exactBytes("synthetic pool password"))
            assertInstanceOf(AccountInsert.Stored::class.java, fixture.accounts.insert(id, identity, hash, AccountRole.VIEWER))
            fixture.owner.pool("core").use { pool ->
                pool.maximumPoolSize = maximum
                val registry = KeyRegistry(pool)
                val crypto = AccountCryptography(registry, fixture.keys, NonceAllocator(pool), AeadCipher(), fixture.alert)
                val accounts = ProtectedAccounts(pool, registry, crypto, AccountPayload(), fixture.alert)
                val barrier = CyclicBarrier(8)
                val executor = Executors.newFixedThreadPool(8)
                try {
                    val reads =
                        (1..8).map {
                            executor.submit<ProtectedRead<*>> {
                                barrier.await(10, TimeUnit.SECONDS)
                                accounts.identity(id)
                            }
                        }
                    reads.forEach {
                        val result = assertDoesNotThrow<ProtectedRead<*>> { it.get(10, TimeUnit.SECONDS) }
                        val stored = assertInstanceOf(ProtectedRead.Value::class.java, result).value as AccountIdentity
                        assertEquals(identity.name, stored.name)
                        assertEquals(identity.emailDisplay, stored.emailDisplay)
                        assertEquals(identity.emailNormalized, stored.emailNormalized)
                    }
                    assertEquals(0, pool.hikariPoolMXBean.activeConnections)
                    assertEquals(1, fixture.scalar("SELECT count(*) FROM appdata.user_account"))
                    fixture.observe("t005a1", "bounded-read-pool-$maximum", mapOf("reads" to 8, "maximumPoolSize" to maximum))
                } finally {
                    executor.shutdownNow()
                    executor.awaitTermination(10, TimeUnit.SECONDS)
                }
            }
        }
}
