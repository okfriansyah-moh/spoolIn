package com.spoolin.core.features.session

import com.spoolin.core.Application
import com.spoolin.core.features.account.application.AuthenticationAdmission
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
@Profile("t006-identity-fixture")
class AdmittedIdentityFixture {
    @Bean
    @Primary
    fun fixtureAdmission(): AuthenticationAdmission = AuthenticationAdmission { _, _ -> }
}

class IdentityHttpFixture(
    admitted: Boolean,
) : AutoCloseable {
    val crypto = CryptoFixture("t006-identity-https.json")
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
        if (admitted) app.setAdditionalProfiles("t006-identity-fixture")
        app.setDefaultProperties(mapOf("spring.main.banner-mode" to "off", "logging.level.root" to "ERROR"))
        context = app.run(*configuration().toTypedArray())
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
        csrf?.let { builder.header("X-CSRF", it) }
        if (cookies.isNotEmpty()) builder.header("Cookie", cookies.entries.joinToString("; ") { "${it.key}=${it.value}" })
        val response = client.send(builder.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString())
        response.headers().allValues("Set-Cookie").forEach {
            val pair = it.substringBefore(';').split('=', limit = 2)
            if (pair[1].isEmpty()) cookies.remove(pair[0]) else cookies[pair[0]] = pair[1]
        }
        return response
    }

    private fun configuration(): List<String> {
        val owner = crypto.owner
        val keys =
            listOf("csrf-key", "operation-key").associateWith { name ->
                owner.root.resolve(name).also {
                    Files.write(it, ByteArray(32) { index -> (index + name.length).toByte() })
                    Files.setPosixFilePermissions(it, PosixFilePermissions.fromString("rw-------"))
                }
            }
        val settings =
            listOf(
                "CORE_DB_URL=${owner.url("core")}",
                "CORE_DB_USER=spoolin_core_runtime",
                "CORE_DB_PASSWORD_FILE=${owner.passwords.getValue("core-runtime")}",
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
                "OPERATION_HMAC_KEY_ID=v1",
                "OPERATION_HMAC_KEY_FILE=${keys.getValue("operation-key")}",
                "spoolin.operations.comparison-keys.v1=${keys.getValue("operation-key")}",
                "BROWSER_ORIGIN=$origin",
            )
        val mounts =
            crypto.mounts.flatMap { (id, mount) ->
                listOf(
                    "spoolin.account-protection.keys.$id.purpose=${mount.purpose}",
                    "spoolin.account-protection.keys.$id.file=${mount.file}",
                )
            }
        return (settings + mounts).map { "--$it" }
    }

    override fun close() {
        context.close()
        crypto.close()
    }
}
