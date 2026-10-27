package com.spoolin.core.boundary.graphql

import com.spoolin.core.boundary.http.BrowserContext
import com.spoolin.core.features.account.application.AuthenticationAdmission
import com.spoolin.core.features.account.application.AuthenticationConfiguration
import com.spoolin.core.features.account.application.AuthenticationRequest
import com.spoolin.core.features.account.persistence.AccountPayload
import com.spoolin.core.features.account.persistence.AccountProtectionConfiguration
import com.spoolin.core.features.account.persistence.AccountProtectionSettings
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.crypto.AeadCipher
import com.spoolin.core.features.crypto.Argon2Engine
import com.spoolin.core.features.crypto.KeyMount
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.ProtectionAlert
import com.spoolin.core.features.crypto.ProtectionBinding
import com.spoolin.core.features.crypto.ProtectionUnavailable
import com.spoolin.core.features.ratelimit.RateAdmission
import com.spoolin.core.features.ratelimit.RateAgreement
import com.spoolin.core.features.ratelimit.TrustedAdmission
import com.spoolin.core.features.session.AnonymousCsrf
import com.spoolin.core.features.session.SessionCredential
import com.spoolin.core.features.session.SessionCsrf
import com.spoolin.core.features.session.SessionJdbcFixture
import com.spoolin.core.features.session.SessionPrincipal
import com.spoolin.core.operations.receipts.OwnerReceipts
import com.spoolin.support.identity.RateProfiles
import com.spoolin.support.policy.ConfigurationSources
import com.spoolin.support.receipts.IntentHashes
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.RegistrationIntent
import com.spoolin.support.receipts.SafeResult
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironmentImpl
import graphql.schema.idl.RuntimeWiring
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class AccountGraphqlBehaviorTest {
    @TempDir lateinit var root: Path
    private val account = UUID.randomUUID()
    private val operation = UUID.randomUUID()
    private val session = UUID.randomUUID()
    private val now = Instant.now()
    private val hashes = IntentHashes(mapOf("compare" to ByteArray(32) { 1 }), "compare")
    private val identity = IdentityRules().identity("Person", "person@example.test")
    private val password = "correct horse battery staple"
    private val pii = ByteArray(32) { 79 }

    @Test
    fun `anonymous and authenticated session expose only browser identity and current csrf`() =
        fixture { h ->
            val anonymous = AnonymousCsrf(ByteArray(32) { 2 }).issue()
            val result = h.call("Query", "session", BrowserContext(null, anonymous, null, null), emptyMap()) as Map<*, *>
            assertEquals(false, result["authenticated"])
            assertEquals(anonymous.csrfToken, result["csrfToken"])
            val principal =
                SessionPrincipal(
                    session,
                    com.spoolin.core.identity
                        .AccountAuthority(account, com.spoolin.core.identity.AccountRole.VIEWER, 3),
                    now.plusSeconds(3600),
                    "csrf",
                )
            val envelope =
                AeadCipher().encrypt(
                    "pii",
                    pii,
                    ByteArray(12) {
                        2
                    },
                    ProtectionBinding("core", "user_account", account, KeyPurpose.PII),
                    AccountPayload().encode(identity),
                )
            h.db.enqueue(mapOf("pii_key_id" to "pii", "pii_nonce" to envelope.nonce(), "pii_cipher" to envelope.cipher()))
            h.db.enqueue(
                mapOf("id" to "pii"),
            )
            val authenticated = h.call("Query", "session", BrowserContext(principal, null, null, null), emptyMap()) as Map<*, *>
            assertEquals(true, authenticated["authenticated"])
            assertEquals("Person", authenticated["displayName"])
            assertEquals("VIEWER", authenticated["role"])
            assertFalse(authenticated.containsKey("email"))
            h.db.enqueue()
            assertThrows(
                ProtectionUnavailable::class.java,
            ) { h.call("Query", "session", BrowserContext(principal, null, null, null), emptyMap()) }
        }

    @Test
    fun `registration runs real admission hashing encryption persistence and durable receipt`() =
        fixture { h ->
            val anonymous = AnonymousCsrf(ByteArray(32) { 2 }).issue()
            val intent = RegistrationIntent.create(operation, anonymous.scope, "registration-v1", identity.emailNormalized, anonymous.nonce)
            h.rate(1)
            h.db.enqueue()
            h.db.enqueue(mapOf("id" to "pii"))
            h.db.enqueue(mapOf("allocated" to 1L))
            h.db.enqueue(receipt(intent, "PENDING"))
            h.db.enqueue(mapOf("id" to "pii"))
            h.db.enqueue(
                mapOf(
                    "id" to "lookup",
                    "state" to "WRITE",
                ),
            )
            h.db.enqueue(receipt(intent, "SUCCEEDED"))
            val context = BrowserContext(null, anonymous, null, null, AuthenticationRequest("127.0.0.1", null))
            val result =
                h.call(
                    "Mutation",
                    "register",
                    context,
                    mapOf("input" to mapOf("name" to identity.name, "email" to identity.emailDisplay, "password" to password)),
                ) as Map<*, *>
            assertEquals("SUCCEEDED", (result["operation"] as Map<*, *>)["state"])
            assertNull(result["problem"])
            assertTrue(context.cookies().isEmpty())
            assertEquals(3, h.db.commits)
        }

    @Test
    fun `sign in verifies protected credential and returns new secure browser cookie`() =
        fixture { h ->
            val anonymous = AnonymousCsrf(ByteArray(32) { 2 }).issue()
            val intent =
                OperationIntent(
                    operation,
                    anonymous.scope,
                    "SignIn",
                    "signin-v1",
                    listOf(
                        "account" to account.toString(),
                        "nonce" to anonymous.nonce.toString(),
                    ),
                )
            h.rate(2)
            h.db.enqueue(mapOf("id" to "lookup", "state" to "WRITE"))
            val encoded = Argon2Engine().hash(password.toByteArray()).encoded
            h.db.enqueue(mapOf("id" to account, "password_hash" to encoded, "auth_epoch" to 3L))
            h.db.enqueue(receipt(intent, "PENDING"))
            repeat(2) { h.db.enqueue(mapOf("role" to "VIEWER", "auth_epoch" to 3L)) }
            h.db.enqueue(receipt(intent, "SUCCEEDED"))
            val context = BrowserContext(null, anonymous, null, null, AuthenticationRequest("127.0.0.1", null))
            val result = h.call("Mutation", "signIn", context, mapOf("email" to identity.emailDisplay, "password" to password)) as Map<*, *>
            assertEquals("SUCCEEDED", (result["operation"] as Map<*, *>)["state"])
            assertEquals(1, context.cookies().size)
            assertTrue(context.cookies().single().contains("Secure; HttpOnly; SameSite=Lax; Path=/"))
            assertFalse(context.cookies().single().contains(password))
        }

    @Test
    fun `sign out validates csrf revokes live session and issues deletion only after receipt succeeds`() =
        fixture { h ->
            val credential = SessionCredential.issue()
            val csrf = h.csrf.derive(credential)
            val intent = OperationIntent(operation, "account:$account", "SignOut", "signout-v1", listOf("session" to session.toString()))
            h.rate(1)
            h.db.enqueue(mapOf("id" to session, "account_id" to account))
            h.db.enqueue(mapOf("account_id" to account))
            h.db.enqueue(mapOf("role" to "VIEWER", "auth_epoch" to 3L))
            h.db.enqueue(
                mapOf(
                    "id" to session,
                    "auth_epoch" to 3L,
                    "csrf_hash" to h.csrf.digest(csrf),
                    "created_at" to Timestamp.from(now.minusSeconds(10)),
                    "last_seen_at" to Timestamp.from(now),
                    "expires_at" to Timestamp.from(now.plusSeconds(3600)),
                    "revoked_at" to null,
                ),
            )
            h.db.enqueue(receipt(intent, "PENDING"))
            h.db.enqueue(receipt(intent, "SUCCEEDED"))
            val context = BrowserContext(null, null, credential, csrf, AuthenticationRequest("127.0.0.1", session))
            val result = h.call("Mutation", "signOut", context, emptyMap()) as Map<*, *>
            assertEquals("SUCCEEDED", (result["operation"] as Map<*, *>)["state"])
            assertEquals(listOf(SessionCredential.deletion()), context.cookies())
        }

    private fun receipt(
        intent: OperationIntent,
        state: String,
    ): Map<String, Any?> =
        mapOf(
            "id" to operation,
            "state" to state,
            "expires_at" to Timestamp.from(now.plusSeconds(3600)),
            "created_at" to Timestamp.from(now),
            "result" to
                if (state ==
                    "SUCCEEDED"
                ) {
                    SafeResult(operation).json()
                } else {
                    null
                },
            "error_code" to null,
            "correlation_id" to UUID.randomUUID(),
            "actor_scope" to intent.actorScope,
            "kind" to intent.kind,
            "request_hash" to hashes.hash(intent, "compare"),
            "hash_key_id" to "compare",
            "receipt_type" to "REQUEST",
        )

    private inner class Harness(
        val db: SessionJdbcFixture,
        val wiring: RuntimeWiring,
        val csrf: SessionCsrf,
    ) {
        fun rate(windows: Int) {
            db.enqueue(mapOf("1" to now.epochSecond))
            repeat(windows) { db.enqueue(emptyMap()) }
        }

        fun call(
            type: String,
            field: String,
            context: BrowserContext,
            args: Map<String, Any?>,
        ): Any? =
            wiring.dataFetchers.getValue(type).getValue(field).get(
                DataFetchingEnvironmentImpl
                    .newDataFetchingEnvironment()
                    .arguments(
                        mapOf("operationId" to operation.toString()) + args,
                    ).graphQLContext(GraphQLContext.newContext().of("browser", context).build())
                    .build(),
            )
    }

    private fun fixture(test: (Harness) -> Unit) {
        val mounts =
            listOf(
                Triple("pii", KeyPurpose.PII, pii),
                Triple("lookup", KeyPurpose.EMAIL_LOOKUP, ByteArray(32) { 83 }),
            ).associate { (id, purpose, bytes) ->
                val path = root.resolve(id)
                Files.write(path, bytes)
                protect(path)
                id to KeyMount(purpose, path)
            }
        val rateKey = ByteArray(32) { 89 }
        val profiles = RateProfiles(ConfigurationSources())
        val agreement = root.resolve("agreement.json")
        val canary =
            Mac
                .getInstance("HmacSHA256")
                .also {
                    it.init(SecretKeySpec(rateKey, "HmacSHA256"))
                }.doFinal("spoolin-rate-agreement-v1".toByteArray())
        Files.writeString(
            agreement,
            "{\"profileRevision\":\"${profiles.revision}\",\"keyCanary\":\"${java.util.HexFormat.of().formatHex(canary)}\"}",
        )
        protect(agreement)
        val db = SessionJdbcFixture(emptyList())
        val alert = ProtectionAlert { }
        val csrf = SessionCsrf(ByteArray(32) { 3 })
        val admission =
            AuthenticationAdmission(
                TrustedAdmission(RateAdmission(db, profiles, rateKey, RateAgreement(agreement, profiles.revision, rateKey)::check))::check,
            )
        val protection = AccountProtectionConfiguration()
        val authentication = AuthenticationConfiguration()
        val provider =
            com.spoolin.core.features.policy
                .PolicyProvider(
                    com.spoolin.support.policy.PolicyEnvironment
                        .resolve { null },
                )
        protection.mountedKeys(AccountProtectionSettings(mounts)).use { keys ->
            protection.passwordHasher(provider).use { hasher ->
                val registry = protection.keyRegistry(db)
                val nonces = protection.nonceAllocator(db)
                val crypto = protection.accountCryptography(registry, keys, nonces, alert)
                val rules = protection.identityRules(provider)
                val accounts = protection.protectedAccounts(db, registry, crypto, alert, rules)
                val sessions = authentication.sessionStore(db, csrf)
                val receipts = OwnerReceipts(db, hashes)
                val registration = authentication.registerAccount(accounts, hasher, rules, receipts, admission)
                val verifier = authentication.credentialVerifier(accounts, hasher, rules)
                val login = authentication.signInAccount(verifier, sessions, receipts, admission)
                val logout = authentication.signOutAccount(sessions, receipts, admission)
                val wiring = RuntimeWiring.newRuntimeWiring()
                authentication.accountGraphql(accounts, registration, login, logout).bind(wiring)
                test(Harness(db, wiring.build(), csrf))
            }
        }
    }

    private fun protect(path: Path) {
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"))
    }
}
