package com.spoolin.catalog.features.lifecycle

import com.spoolin.support.receipts.queryBound
import io.grpc.Status
import java.io.DataOutputStream
import java.io.OutputStream
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.sql.Connection
import java.util.UUID

/** Complete owner snapshot. Capacity rejection never issues a token for a truncated relationship set. */
data class ImpactSnapshot(
    val person: UUID, val version: Long, val impactVersion: Long, val status: String,
    val credits: List<ImpactCredit>,
) {
    fun digest(): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        DataOutputStream(DigestOutputStream(OutputStream.nullOutputStream(), digest)).use { output ->
            output.writeUTF(person.toString()); output.writeLong(version); output.writeLong(impactVersion)
            output.writeUTF(status); output.writeInt(credits.size)
            credits.sortedBy { it.id.toString() }.forEach { credit ->
                output.writeUTF(credit.id.toString()); output.writeLong(credit.version)
                output.writeUTF(credit.title.toString()); output.writeLong(credit.titleVersion)
                output.writeUTF(credit.titleName); output.writeUTF(credit.titleKind); output.writeUTF(credit.titleStatus)
            }
        }
        return digest.digest()
    }

    companion object {
        const val MAX_CREDITS = 10000
        fun read(connection: Connection, person: UUID): ImpactSnapshot {
            val subject = connection.queryBound("SELECT version,impact_version,status FROM person WHERE id=? AND status<>'DELETED'",
                listOf(person)) { Triple(it.getLong("version"), it.getLong("impact_version"), it.getString("status")) }
                .singleOrNull() ?: throw Status.NOT_FOUND.asRuntimeException()
            val credits = connection.queryBound("SELECT c.id,c.version,c.title_id,t.version AS title_version," +
                "t.display_title,t.kind,t.status FROM credit c JOIN title t ON t.id=c.title_id WHERE c.person_id=? " +
                "ORDER BY c.id LIMIT ?", listOf(person, MAX_CREDITS + 1)) { row ->
                ImpactCredit(row.getObject("id", UUID::class.java), row.getLong("version"),
                    row.getObject("title_id", UUID::class.java), row.getLong("title_version"),
                    row.getString("display_title"), row.getString("kind"), row.getString("status"))
            }
            if (credits.size > MAX_CREDITS || credits.any { it.titleName.toByteArray(Charsets.UTF_8).size > 4096 } || credits.sumOf { it.titleName.length.toLong() } > 3000000) {
                throw Status.RESOURCE_EXHAUSTED.withDescription("IMPACT_CAPACITY_EXCEEDED").asRuntimeException()
            }
            return ImpactSnapshot(person, subject.first, subject.second, subject.third, credits)
        }
    }
}

data class ImpactCredit(
    val id: UUID, val version: Long, val title: UUID, val titleVersion: Long,
    val titleName: String, val titleKind: String, val titleStatus: String,
)
