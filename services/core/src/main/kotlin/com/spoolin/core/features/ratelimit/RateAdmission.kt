package com.spoolin.core.features.ratelimit

import com.spoolin.support.identity.RateProfile
import com.spoolin.support.identity.RateProfiles
import com.spoolin.support.identity.RateScope
import com.spoolin.support.receipts.committed
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.sql.Connection
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.sql.DataSource

private const val EXPIRY_PARAMETER = 3
private const val MAXIMUM_PARAMETER = 4
private const val RATE_KEY_BYTES = 32
private const val MILLIS_PER_SECOND = 1000L

class RateLimited(
    val retryAfterMs: Long,
) : IllegalStateException("Request limit reached")

class RateUnavailable : IllegalStateException("Request admission unavailable")

data class RateDemand(
    val profile: RateProfile,
    val scope: RateScope,
    internal val identity: String,
) {
    init {
        require(identity.isNotEmpty())
    }

    override fun toString(): String = "RateDemand(redacted)"
}

class RateAdmission(
    private val dataSource: DataSource,
    private val profiles: RateProfiles,
    secret: ByteArray,
    private val agreement: () -> Unit = { throw RateUnavailable() },
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(RateAdmission::class.java)
    private val key = secret.copyOf().also { require(it.size == RATE_KEY_BYTES) }

    fun admit(demands: List<RateDemand>) {
        require(demands.isNotEmpty())
        agreement()
        val retry =
            try {
                dataSource.committed { connection -> consume(connection, demands) }
            } catch (failure: Exception) {
                logger.warn("Rate admission failed", failure)
                throw RateUnavailable()
            }
        if (retry > 0) throw RateLimited(retry)
    }

    private fun consume(
        connection: Connection,
        demands: List<RateDemand>,
    ): Long {
        val now =
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT floor(extract(epoch from clock_timestamp()))::bigint").use { rows ->
                    check(rows.next())
                    rows.getLong(1)
                }
            }
        val buckets = demands.map { demand -> bucket(demand, now) }.distinctBy { it.order }.sortedBy { it.order }
        return buckets
            .maxOf { bucket ->
                if (consume(connection, bucket)) 0L else (bucket.start + bucket.seconds - now) * MILLIS_PER_SECOND
            }.coerceAtLeast(0)
    }

    private fun bucket(
        demand: RateDemand,
        now: Long,
    ): RateBucket {
        val rule = profiles.rule(demand.profile)
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { fields ->
            listOf("rate-v1", demand.profile.name, rule.seconds.toString(), demand.scope.name, demand.identity)
                .forEach { field ->
                    val encoded = field.toByteArray(Charsets.UTF_8)
                    fields.writeInt(encoded.size)
                    fields.write(encoded)
                }
        }
        val bytes = output.toByteArray()
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return RateBucket(mac.doFinal(bytes), now / rule.seconds * rule.seconds, rule.seconds, rule.maximum)
    }

    private fun consume(
        connection: Connection,
        bucket: RateBucket,
    ): Boolean =
        connection
            .prepareStatement(
                "INSERT INTO appdata.request_rate_window(bucket_key,window_start,hits,expires_at) VALUES (?,?,1,?) " +
                    "ON CONFLICT (bucket_key,window_start) DO UPDATE SET hits=request_rate_window.hits+1 " +
                    "WHERE request_rate_window.hits < ? RETURNING hits",
            ).use { statement ->
                statement.setBytes(1, bucket.key)
                statement.setObject(2, java.sql.Timestamp.from(Instant.ofEpochSecond(bucket.start)))
                statement.setObject(
                    EXPIRY_PARAMETER,
                    java.sql.Timestamp.from(Instant.ofEpochSecond(bucket.start + 2L * bucket.seconds)),
                )
                statement.setInt(MAXIMUM_PARAMETER, bucket.maximum)
                statement.executeQuery().use { it.next() }
            }
}

private class RateBucket(
    val key: ByteArray,
    val start: Long,
    val seconds: Int,
    val maximum: Int,
) {
    val order: String = key.joinToString("") { "%02x".format(it) } + ":$start"
}
