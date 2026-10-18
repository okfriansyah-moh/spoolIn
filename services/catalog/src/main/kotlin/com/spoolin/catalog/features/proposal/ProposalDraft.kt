package com.spoolin.catalog.features.proposal

import com.spoolin.catalog.features.movie.MovieValidation
import spoolin.v1.Spoolin

internal object ProposalDraft {
    fun normalize(request: Spoolin.Proposal): Spoolin.Proposal {
        val valid = MovieValidation.validateCreate(request.values)
        val values = Spoolin.MovieValues.newBuilder()
            .setTitle(valid.displayTitle).setSynopsis(valid.synopsis).setSynopsisLanguage(valid.synopsisLanguage)
            .addAllCountries(valid.countries).addAllGenreIds(valid.genreIds.sorted())
        valid.originalTitle?.let(values::setOriginalTitle)
        valid.originalLanguage?.let(values::setOriginalLanguage)
        valid.runtimeMinutes?.let(values::setRuntimeMinutes)
        valid.releaseYear?.let { year ->
            val date = Spoolin.PartialDate.newBuilder().setYear(year)
            valid.releaseMonth?.let(date::setMonth)
            valid.releaseDay?.let(date::setDay)
            values.release = date.build()
        }
        return request.toBuilder().setValues(values).build()
    }
}
