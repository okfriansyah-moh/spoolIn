package com.spoolin.core.features.session

import com.spoolin.core.Application
import com.spoolin.core.features.account.application.AuthenticationAction
import com.spoolin.core.features.account.application.AuthenticationAdmission
import com.spoolin.core.features.account.application.AuthenticationDenied
import com.spoolin.core.features.account.application.AuthenticationRequest
import com.spoolin.core.recovery.crypto.CryptoFixture
import org.springframework.boot.SpringApplication
import org.springframework.boot.ssl.SslBundles
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.time.Duration

@Configuration(proxyBeanMethods = false)
@Profile("t008-identity-fixture")
class AdmittedIdentityFixture {
    @Bean
    @Primary
    fun fixtureAdmission(): AdmissionFixture = AdmissionFixture()
}

class AdmissionFixture : AuthenticationAdmission {
    val entered = java.util.concurrent.CountDownLatch(8)
    val release = java.util.concurrent.CountDownLatch(1)
    val observed = java.util.concurrent.CopyOnWriteArrayList<AuthenticationRequest>()

    @Volatile var block = false

    override fun check(
        action: AuthenticationAction,
        identity: String?,
        request: AuthenticationRequest,
    ) {
        observed.add(request)
        if (block) {
            entered.countDown()
            try {
                check(release.await(10, java.util.concurrent.TimeUnit.SECONDS))
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                throw AuthenticationDenied()
            }
        }
    }
}

class IdentityHttpFixture(
    private val admitted: Boolean,
    private val trusted: Boolean = false,
    private val rateOverrides: Map<String, String> = emptyMap(),
    private val sharedCrypto: CryptoFixture? = null,
) : AutoCloseable {
    val crypto = sharedCrypto ?: CryptoFixture("t008-identity-https.json")
    private val port = ServerSocket(0).use { it.localPort }
    val origin = "https://localhost:$port"
    val context: ConfigurableApplicationContext
    val client: HttpClient
    val cookies = mutableMapOf<String, String>()

    init {
        val app =
            if (admitted) {
                SpringApplication(Application::class.java, AdmittedIdentityFixture::class.java)
            } else {
                SpringApplication(Application::class.java)
            }
        if (admitted) app.setAdditionalProfiles("t008-identity-fixture")
        app.setDefaultProperties(mapOf("spring.main.banner-mode" to "off", "logging.level.root" to "ERROR"))
        context =
            try {
                app.run(*configuration().toTypedArray())
            } catch (failure: Exception) {
                if (sharedCrypto == null) crypto.close()
                throw failure
            }
        val ssl = context.getBean(SslBundles::class.java).getBundle("service").createSslContext()
        client = HttpClient.newBuilder().sslContext(ssl).build()
    }

    fun request(
        body: String,
        csrf: String? = null,
        originValue: String? = origin,
    ): HttpResponse<String> {
        val builder =
            HttpRequest
                .newBuilder(URI("$origin/graphql"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/graphql-response+json, application/json")
                .header("X-Spoolin-Client", "web")
                .timeout(Duration.ofSeconds(5))
        originValue?.let { builder.header("Origin", it) }
        csrf?.let { builder.header("X-CSRF-Token", it) }
        if (cookies.isNotEmpty()) builder.header("Cookie", cookies.entries.joinToString("; ") { "${it.key}=${it.value}" })
        val response = client.send(builder.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString())
        response.headers().allValues("Set-Cookie").forEach {
            val pair = it.substringBefore(';').split('=', limit = 2)
            if (pair[1].isEmpty()) cookies.remove(pair[0]) else cookies[pair[0]] = pair[1]
        }
        return response
    }

    fun raw(
        body: String,
        headers: Map<String, String>,
        method: String = "POST",
        repeatCsrf: String? = null,
        timeout: Duration = Duration.ofSeconds(5),
    ): HttpResponse<String> {
        val builder = HttpRequest.newBuilder(URI("$origin/graphql")).timeout(timeout)
        headers.forEach { (name, value) -> builder.header(name, value) }
        repeatCsrf?.let { builder.header("X-CSRF-Token", it) }
        return client.send(
            builder.method(method, HttpRequest.BodyPublishers.ofString(body)).build(),
            HttpResponse.BodyHandlers.ofString(),
        )
    }

    fun wrongHost(): Pair<Int, Map<String, String>> {
        val socket = client.sslContext().socketFactory.createSocket("localhost", port) as javax.net.ssl.SSLSocket
        socket.use {
            val parameters = it.sslParameters
            parameters.endpointIdentificationAlgorithm = "HTTPS"
            it.sslParameters = parameters
            it.soTimeout = 5000
            it.startHandshake()
            val body = """{"query":"query { policy { revision } }"}"""
            val message =
                "POST /graphql HTTP/1.1\r\nHost: foreign.example\r\nConnection: close\r\n" +
                    "Origin: $origin\r\nX-Spoolin-Client: web\r\nContent-Type: application/json\r\n" +
                    "Content-Length: ${body.toByteArray(Charsets.UTF_8).size}\r\n\r\n$body"
            it.outputStream.write(message.toByteArray(Charsets.UTF_8))
            it.outputStream.flush()
            val reader = it.inputStream.bufferedReader()
            val status = reader.readLine().split(' ')[1].toInt()
            val headers = mutableMapOf<String, String>()
            var line = reader.readLine()
            while (!line.isNullOrEmpty()) {
                headers[line.substringBefore(':').lowercase()] = line.substringAfter(':').trim()
                line = reader.readLine()
            }
            return status to headers
        }
    }

    private fun configuration(): List<String> {
        val owner = crypto.owner
        val keys = fixtureKeys(owner)
        val rateValues = mapOf("RATE_LIMIT_READ_MAX_REQUESTS" to "1000000") + rateOverrides
        val agreement = fixtureAgreement(owner, keys, rateValues)
        return (identitySettings(owner, keys, agreement) + protectionMounts() + rateValues.map { "${it.key}=${it.value}" }).map { "--$it" }
    }

    private fun fixtureKeys(owner: com.spoolin.core.boundary.bootstrap.OwnerFixture): Map<String, java.nio.file.Path> =
        listOf("csrf-key", "operation-key", "rate-limiter-key").associateWith { name ->
            owner.root.resolve(name).also {
                Files.write(it, ByteArray(32) { index -> (index + name.length).toByte() })
                Files.setPosixFilePermissions(it, PosixFilePermissions.fromString("rw-------"))
            }
        }

    private fun fixtureAgreement(
        owner: com.spoolin.core.boundary.bootstrap.OwnerFixture,
        keys: Map<String, java.nio.file.Path>,
        rateValues: Map<String, String>,
    ): java.nio.file.Path {
        val rateProfiles =
            com.spoolin.support.identity
                .RateProfiles(
                    com.spoolin.support.policy
                        .ConfigurationSources(rateValues),
                )
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(javax.crypto.spec.SecretKeySpec(Files.readAllBytes(keys.getValue("rate-limiter-key")), "HmacSHA256"))
        val canary = mac.doFinal("spoolin-rate-agreement-v1".toByteArray()).joinToString("") { "%02x".format(it) }
        val agreement = owner.root.resolve("rate-agreement.json")
        if (!Files.exists(agreement)) {
            Files.writeString(agreement, """{"profileRevision":"${rateProfiles.revision}","keyCanary":"$canary"}""")
            Files.setPosixFilePermissions(agreement, PosixFilePermissions.fromString("rw-------"))
        }
        return agreement
    }

    private fun identitySettings(
        owner: com.spoolin.core.boundary.bootstrap.OwnerFixture,
        keys: Map<String, java.nio.file.Path>,
        agreement: java.nio.file.Path,
    ): List<String> =
        listOf(
            "CORE_DB_URL=${owner.url("core")}",
            "CORE_DB_USER=spoolin_core_runtime",
            "CORE_DB_PASSWORD_FILE=${owner.passwords.getValue("core-runtime")}",
            "CORE_DB_MAINTENANCE_PASSWORD_FILE=${owner.passwords.getValue("core-maintenance")}",
            "SERVICE_CERT_FILE=${owner.root.resolve("core.crt")}",
            "SERVICE_KEY_FILE=${owner.root.resolve("core.key")}",
            "SERVICE_CA_FILE=${owner.root.resolve("ca.crt")}",
            "HTTP_ADDRESS=127.0.0.1",
            "HTTP_PORT=$port",
            "GRPC_ADDRESS=127.0.0.1",
            "GRPC_PORT=0",
            "MANAGEMENT_ADDRESS=127.0.0.1",
            "MANAGEMENT_PORT=0",
            "spoolin.account-protection.enabled=true",
            "spoolin.operations.enabled=true",
            "CSRF_KEY_FILE=${keys.getValue("csrf-key")}",
            "RATE_LIMIT_KEY_FILE=${keys.getValue("rate-limiter-key")}",
            "spoolin.rate-limit.enabled=${admitted || trusted}",
            "spoolin.rate-limit.agreement-file=$agreement",
            "OPERATION_HMAC_KEY_ID=v1",
            "OPERATION_HMAC_KEY_FILE=${keys.getValue("operation-key")}",
            "spoolin.operations.comparison-keys.v1=${keys.getValue("operation-key")}",
            "BROWSER_ORIGIN=$origin",
        )

    private fun protectionMounts(): List<String> =
        crypto.mounts.flatMap { (id, mount) ->
            listOf(
                "spoolin.account-protection.keys.$id.purpose=${mount.purpose}",
                "spoolin.account-protection.keys.$id.file=${mount.file}",
            )
        }

    override fun close() {
        context.close()
        if (sharedCrypto == null) crypto.close()
    }
}
