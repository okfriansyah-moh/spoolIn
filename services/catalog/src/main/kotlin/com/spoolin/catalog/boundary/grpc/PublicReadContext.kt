package com.spoolin.catalog.boundary.grpc

import com.spoolin.catalog.features.person.read.PersonSearch
import com.spoolin.catalog.features.search.CatalogCursor
import com.spoolin.catalog.features.search.PeopleCursor
import com.spoolin.support.policy.InputViolation
import com.spoolin.support.policy.PublicPolicy
import com.spoolin.support.policy.ReadPolicy
import io.grpc.Status
import spoolin.v1.Spoolin.Search
import java.time.Clock
import java.util.UUID

internal class PublicReadContext(
    val titleCursor: CatalogCursor,
    val peopleCursor: PeopleCursor,
    val clock: Clock,
    val policy: () -> PublicPolicy,
) {
    /** Scope binds the cursor to a specific target (title or person) to prevent cross-use. */
    fun creditScope(
        targetId: UUID,
        kind: String,
        first: Int,
    ): String =
        ReadPolicy(policy()).cursorScope(
            "public-credits",
            first,
            mapOf("kind" to kind, "target" to targetId.toString()),
        )

    fun pageSize(first: Int): Int = readInput { ReadPolicy(policy()).rpcPageSize(first) }

    fun searchText(raw: String?): String? = readInput { ReadPolicy(policy()).search(raw) }

    private fun <T> readInput(read: () -> T): T =
        try {
            read()
        } catch (_: InputViolation) {
            throw Status.INVALID_ARGUMENT.withDescription("FIELD_INVALID").asRuntimeException()
        }

    fun encodeCreditCursor(
        ks: PersonSearch.CreditKeyset,
        scope: String,
    ): String {
        val keyset = CatalogCursor.Keyset(null, null, null, ks.id)
        return titleCursor.encode(keyset, scope, clock.instant())
    }

    fun decodeCreditCursor(
        raw: String,
        scope: String,
    ): PersonSearch.CreditKeyset? {
        val ks = titleCursor.decode(raw, scope, clock.instant()) ?: return null
        return PersonSearch.CreditKeyset(ks.id)
    }

    fun titleAfter(raw: String, scope: String): CatalogCursor.Keyset? =
        raw.takeIf { it.isNotEmpty() }?.let {
            titleCursor.decode(it, scope, clock.instant())
                ?: throw Status.INVALID_ARGUMENT.withDescription("INVALID_CURSOR").asRuntimeException()
        }

    fun peopleScope(request: Search, text: String?, first: Int): String {
        require(!request.hasKind() && request.genreIdsCount == 0 && !request.hasYear() && !request.hasMonth()) {
            "Title filters are not valid for people search"
        }
        return ReadPolicy(policy()).cursorScope("public-people", first, mapOf("text" to text))
    }

    fun peopleAfter(raw: String, scope: String): PersonSearch.PersonKeyset? =
        raw.takeIf { it.isNotEmpty() }?.let {
            val keyset = peopleCursor.decode(it, scope, clock.instant())
                ?: throw Status.INVALID_ARGUMENT.withDescription("INVALID_CURSOR").asRuntimeException()
            PersonSearch.PersonKeyset(keyset.name, keyset.id)
        }

    fun parseId(value: String): UUID =
        runCatching { UUID.fromString(value).also { require(it.toString() == value) } }
            .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("INVALID_ID").asRuntimeException() }
}
