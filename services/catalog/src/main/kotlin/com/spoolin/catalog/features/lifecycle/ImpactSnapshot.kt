package com.spoolin.catalog.features.lifecycle

import com.google.protobuf.CodedOutputStream
import com.spoolin.support.receipts.queryBound
import io.grpc.Status
import tools.jackson.databind.ObjectMapper
import java.io.DataOutputStream
import java.io.OutputStream
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.UUID

/** Every tuple contributes to the digest; response capacity never authorizes a partial consequence set. */
class ImpactSnapshot private constructor(
    private val subject: Subject,
    val creditCount: Long,
    val affectedTitles: List<String>,
    private val fingerprint: ByteArray,
) {
    val person: UUID get() = subject.person
    val version: Long get() = subject.version
    val impactVersion: Long get() = subject.impactVersion
    val status: String get() = subject.status

    private data class Subject(val person: UUID, val version: Long, val impactVersion: Long, val status: String)

    fun digest(): ByteArray = fingerprint.copyOf()

    companion object {
        const val RESPONSE_BYTES = 131072
        private val mapper = ObjectMapper()

        fun exactCount(value: Long): Int {
            if (value !in 0..Int.MAX_VALUE.toLong()) capacity()
            return value.toInt()
        }

        fun fromCredits(
            person: UUID,
            version: Long,
            impactVersion: Long,
            status: String,
            credits: List<ImpactCredit>,
        ): ImpactSnapshot {
            val accumulator = Accumulator(person, version, impactVersion, status)
            credits.sortedBy { it.id.toString() }.forEach(accumulator::add)
            return accumulator.finish()
        }

        fun read(
            connection: Connection,
            person: UUID,
        ): ImpactSnapshot {
            val subject =
                connection
                    .queryBound(
                        "SELECT version,impact_version,status FROM person WHERE id=? AND status<>'DELETED'",
                        listOf(person),
                    ) { Triple(it.getLong("version"), it.getLong("impact_version"), it.getString("status")) }
                    .singleOrNull() ?: throw Status.NOT_FOUND.asRuntimeException()
            val accumulator = Accumulator(person, subject.first, subject.second, subject.third)
            val deadline =
                System.nanoTime() +
                    java.util.concurrent.TimeUnit.SECONDS
                        .toNanos(READ_SECONDS)
            connection
                .prepareStatement(
                    "SELECT c.id,c.version,c.title_id,t.version AS title_version," +
                        "t.display_title,t.kind,t.status FROM credit c JOIN title t ON t.id=c.title_id " +
                        "WHERE c.person_id=? ORDER BY c.id",
                ).use { statement ->
                    statement.setObject(1, person)
                    statement.fetchSize = FETCH_ROWS
                    accumulate(statement, accumulator, deadline)
                }
            return accumulator.finish()
        }

        private fun accumulate(statement: PreparedStatement, accumulator: Accumulator, deadline: Long) {
            statement.executeQuery().use { rows ->
                while (rows.next()) {
                    checkDeadline(deadline)
                    accumulator.add(credit(rows))
                }
            }
        }

        private fun checkDeadline(deadline: Long) {
            if (io.grpc.Context.current().isCancelled) throw Status.CANCELLED.asRuntimeException()
            if (System.nanoTime() >= deadline) throw Status.DEADLINE_EXCEEDED.asRuntimeException()
        }

        private fun credit(rows: ResultSet): ImpactCredit = ImpactCredit(
            rows.getObject("id", UUID::class.java),
            rows.getLong("version"),
            rows.getObject("title_id", UUID::class.java),
            rows.getLong("title_version"),
            rows.getString("display_title"),
            rows.getString("kind"),
            rows.getString("status"),
        )

        private const val READ_SECONDS = 3L
        private const val FETCH_ROWS = 100
        private const val TITLE_PROTOBUF_FIELD = 5

        private fun capacity(): Nothing =
            throw Status.RESOURCE_EXHAUSTED
                .withDescription("IMPACT_RESPONSE_CAPACITY_EXCEEDED")
                .asRuntimeException()
    }

    private class Accumulator(
        val person: UUID,
        val version: Long,
        val impact: Long,
        val status: String,
    ) {
        private val hash = MessageDigest.getInstance("SHA-256")
        private val output = DataOutputStream(DigestOutputStream(OutputStream.nullOutputStream(), hash))
        private val titles = mutableMapOf<UUID, String>()
        private var protobufTitleBytes = 0L
        private var jsonTitleBytes = 2L
        private var count = 0L

        init {
            text(person.toString())
            output.writeLong(version)
            output.writeLong(impact)
            text(status)
        }

        fun add(credit: ImpactCredit) {
            count++
            exactCount(count)
            if (titles.putIfAbsent(credit.title, credit.titleName) == null) {
                protobufTitleBytes += CodedOutputStream.computeStringSize(TITLE_PROTOBUF_FIELD, credit.titleName)
                jsonTitleBytes += mapper.writeValueAsBytes(credit.titleName).size + if (titles.size == 1) 0 else 1
                if (protobufTitleBytes > RESPONSE_BYTES || jsonTitleBytes > RESPONSE_BYTES) capacity()
            }
            text(credit.id.toString())
            output.writeLong(credit.version)
            text(credit.title.toString())
            output.writeLong(credit.titleVersion)
            text(credit.titleName)
            text(credit.titleKind)
            text(credit.titleStatus)
        }

        fun finish(): ImpactSnapshot {
            output.writeLong(count)
            output.close()
            val ordered = titles.entries.sortedWith(compareBy({ it.value }, { it.key.toString() })).map { it.value }
            return ImpactSnapshot(Subject(person, version, impact, status), count, ordered, hash.digest())
        }

        private fun text(value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            output.writeInt(bytes.size)
            output.write(bytes)
        }
    }
}

data class ImpactCredit(
    val id: UUID,
    val version: Long,
    val title: UUID,
    val titleVersion: Long,
    val titleName: String,
    val titleKind: String,
    val titleStatus: String,
)
