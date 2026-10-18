package com.spoolin.catalog.boundary.grpc

import com.spoolin.catalog.features.person.read.PersonSearch
import com.spoolin.catalog.features.provenance.ProvenanceReads
import com.spoolin.catalog.features.search.TitleSearchSql
import spoolin.v1.Spoolin
import spoolin.v1.Spoolin.CreditPage
import spoolin.v1.Spoolin.PartialDate
import spoolin.v1.Spoolin.Person
import spoolin.v1.Spoolin.PersonValues
import spoolin.v1.Spoolin.Provenance
import spoolin.v1.Spoolin.Title
import java.util.UUID

internal class PublicReadProjection(
    private val context: PublicReadContext,
) {
    fun titleProto(
        row: TitleSearchSql.TitleRow,
        genreIds: List<UUID>,
        provenanceRows: List<ProvenanceReads.ProvenanceRow>,
    ): Title =
        Title
            .newBuilder()
            .setId(row.id.toString())
            .setKind(row.kind)
            .setState(row.status)
            .setValues(movieValues(row, genreIds))
            .setVersion(row.version)
            .setTvEnd(partialDateProto(row.endYear, row.endMonth, row.endDay))
            .addAllProvenance(provenanceRows.map { provenanceProto(it) })
            .also { b ->
                row.assetId?.let { b.assetId = it.toString() }
                row.tvStatus?.let { b.tvStatus = it }
                row.releaseContext?.let { b.releaseContext = it }
            }.build()

    private fun movieValues(
        row: TitleSearchSql.TitleRow,
        genreIds: List<UUID>,
    ): Spoolin.MovieValues =
        Spoolin.MovieValues
            .newBuilder()
            .setTitle(row.displayTitle)
            .setSynopsis(row.synopsis)
            .setSynopsisLanguage(row.synopsisLanguage)
            .addAllCountries(row.countries)
            .addAllGenreIds(genreIds.map { it.toString() })
            .setRelease(partialDateProto(row.releaseYear, row.releaseMonth, row.releaseDay))
            .also { b ->
                row.originalTitle?.let { b.originalTitle = it }
                row.originalLanguage?.let { b.originalLanguage = it }
                row.runtimeMinutes?.let { b.runtimeMinutes = it }
            }.build()

    fun personProto(row: PersonSearch.PersonRow): Person =
        Person
            .newBuilder()
            .setId(row.id.toString())
            .setValues(
                PersonValues
                    .newBuilder()
                    .setName(row.name)
                    .also { b -> row.biography?.let { b.biography = it } }
                    .build(),
            ).setState(row.status)
            .setVersion(row.version)
            .build()

    fun creditPageProto(
        page: PersonSearch.CreditPage,
        scope: String,
    ): CreditPage {
        val builder = CreditPage.newBuilder()
        page.rows.forEach { r ->
            builder.addNodes(
                Spoolin.Credit
                    .newBuilder()
                    .setId(r.id.toString())
                    .setTitleId(r.titleId.toString())
                    .setPersonId(r.personId.toString())
                    .setRoleId(r.roleId.toString())
                    .addAllCharacters(r.characters)
                    .setVersion(r.version)
                    .setPersonName(r.personName)
                    .setTitleName(r.titleName)
                    .setRole(
                        Spoolin.CreditRole
                            .newBuilder()
                            .setId(r.roleId.toString())
                            .setCategory(r.roleCategory)
                            .setLabel(r.roleLabel)
                            .setAllowsCharacters(r.roleAllowsCharacters)
                            .build(),
                    ).build(),
            )
        }
        page.keyset?.let { builder.endCursor = context.encodeCreditCursor(it, scope) }
        builder.hasNextPage = page.hasNextPage
        return builder.build()
    }

    private fun partialDateProto(
        year: Int?,
        month: Int?,
        day: Int?,
    ): PartialDate = ProtoHelpers.partialDate(year, month, day)

    private fun provenanceProto(row: ProvenanceReads.ProvenanceRow): Provenance = ProtoHelpers.provenance(row)
}
