package com.spoolin.catalog.features.proposal

import com.google.protobuf.ByteString
import spoolin.v1.Spoolin
import java.util.Base64

internal object ProposalResponse {
    fun fromRow(row: ProposalRow, freshChallenge: String?): Spoolin.ProposalRevision {
        val expiry = com.google.protobuf.Timestamp.newBuilder()
            .setSeconds(row.expiresAt.epochSecond).setNanos(row.expiresAt.nano)
        val result = Spoolin.ProposalRevision.newBuilder()
            .setProposalId(row.id.toString()).setRevision(row.revision)
            .setValues(ProposalContent.movieValues(ProposalContent.parseJson(row.contentJson)))
            .setContentHash(Base64.getEncoder().encodeToString(row.contentHash)).setExpiresAt(expiry)
        freshChallenge?.let { result.uiChallenge = ByteString.copyFromUtf8(it) }
        return result.build()
    }
}
