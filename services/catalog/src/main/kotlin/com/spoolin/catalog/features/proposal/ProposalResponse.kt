package com.spoolin.catalog.features.proposal

import com.google.protobuf.ByteString
import spoolin.v1.Spoolin
import java.util.Base64

internal object ProposalResponse {
    fun fromRow(
        row: ProposalRow,
        freshChallenge: String?,
        connection: java.sql.Connection? = null,
    ): Spoolin.ProposalRevision {
        val expiry =
            com.google.protobuf.Timestamp
                .newBuilder()
                .setSeconds(row.expiresAt.epochSecond)
                .setNanos(row.expiresAt.nano)
        val result =
            Spoolin.ProposalRevision
                .newBuilder()
                .setProposalId(row.id.toString())
                .setRevision(row.revision)
                .setValues(ProposalContent.movieValues(ProposalContent.parseJson(row.contentJson)))
                .setContentHash(Base64.getEncoder().encodeToString(row.contentHash))
                .setExpiresAt(expiry)
        val content = ProposalContent.parseJson(row.contentJson)
        result.addAllSources(ProposalContent.sources(content)).setState(if (row.state == "REVIEW" && !java.time.Instant.now().isBefore(row.expiresAt)) "EXPIRED" else row.state).setOperationId(row.operationId.toString())
        result.addAllCredits(ProposalContent.credits(content).map { if (connection == null) it else ProposalCredits.labels(connection,it) })
        ProposalContent.poster(content)?.let(result::setPoster)
        freshChallenge?.let { result.uiChallenge = ByteString.copyFromUtf8(it) }
        return result.build()
    }
}
