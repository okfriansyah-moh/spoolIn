package com.spoolin.catalog.boundary.grpc

import com.google.protobuf.Timestamp
import com.spoolin.catalog.features.provenance.ProvenanceReads
import spoolin.v1.Spoolin.PartialDate
import spoolin.v1.Spoolin.Provenance

/** Shared protobuf mapping helpers for catalog gRPC responses. */
internal object ProtoHelpers {
    fun partialDate(
        year: Int?,
        month: Int?,
        day: Int?,
    ): PartialDate =
        PartialDate
            .newBuilder()
            .also { b ->
                year?.let { b.year = it }
                month?.let { b.month = it }
                day?.let { b.day = it }
            }.build()

    fun provenance(row: ProvenanceReads.ProvenanceRow): Provenance {
        val ts =
            Timestamp
                .newBuilder()
                .setSeconds(row.retrievedAt.epochSecond)
                .setNanos(row.retrievedAt.nano)
                .build()
        return Provenance
            .newBuilder()
            .setSource(row.source)
            .setExternalId(row.externalId)
            .setRetrievedAt(ts)
            .setReuseNote(row.reuseNote)
            .addAllLocallyChanged(row.locallyChanged)
            .addAllFieldSources(FieldSourceProjection.parse(row.fieldSources))
            .also { b -> row.recordUrl?.let { b.recordUrl = it } }
            .build()
    }
}
