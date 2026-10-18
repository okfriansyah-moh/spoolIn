package com.spoolin.catalog.features.person.read

import com.spoolin.catalog.features.search.CatalogCursor
import com.spoolin.support.policy.PublicPolicy
import com.spoolin.support.policy.PolicyEnvironment
import com.spoolin.support.policy.ReadPolicy
import com.spoolin.support.receipts.queryBound
import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource


/**
 * Person and credit reads for the public catalog.
 *
 * Person ordering: name ASC, id ASC (stable).
 * Person visibility: status = 'ACTIVE' only.
 * Credit visibility: both person and title must be ACTIVE.
 */
class PersonSearch(
    private val dataSource: DataSource,
    private val policy: () -> PublicPolicy = { PolicyEnvironment.resolve(System::getenv) },
) {
    data class PersonRow(
        val id: UUID,
        val name: String,
        val biography: String?,
        val status: String,
        val version: Long,
    )

    data class PersonPage(
        val rows: List<PersonRow>,
        val keyset: PersonKeyset?,
        val hasNextPage: Boolean,
    )

    data class PersonKeyset(
        val name: String,
        val id: UUID,
    )

    data class CreditRow(
        val id: UUID,
        val titleId: UUID,
        val personId: UUID,
        val roleId: UUID,
        val characters: List<String>,
        val version: Long,
        val personName: String,
        val titleName: String,
        val roleCategory: String,
        val roleLabel: String,
        val roleAllowsCharacters: Boolean,
    )

    data class CreditPage(
        val rows: List<CreditRow>,
        val keyset: CreditKeyset?,
        val hasNextPage: Boolean,
    )

    data class CreditKeyset(
        val id: UUID,
    )

    fun searchPeople(
        text: String?,
        first: Int,
        after: PersonKeyset?,
    ): PersonPage {
        val limit = ReadPolicy(policy()).pageSize(first)
        val params = mutableListOf<Any?>()
        val where = mutableListOf<String>("p.status = 'ACTIVE'")

        val normalized = ReadPolicy(policy()).search(text)
        if (normalized != null) {
            val pattern = "%" + CatalogCursor.escapeWildcards(normalized) + "%"
            where.add(
                "casefold(p.name COLLATE pg_unicode_fast) " +
                    "LIKE casefold(? COLLATE pg_unicode_fast) ESCAPE '\\'",
            )
            params.add(pattern)
        }

        if (after != null) {
            where.add("(p.name > ? OR (p.name = ? AND p.id > ?))")
            params.add(after.name)
            params.add(after.name)
            params.add(after.id)
        }

        val sql = """SELECT p.id, p.name, p.biography, p.status, p.version
                     FROM person p
                     WHERE ${where.joinToString(" AND ")}
                     ORDER BY p.name ASC, p.id ASC
                     LIMIT ${limit + 1}"""

        val rows =
            dataSource.connection.use { conn ->
                conn.queryBound(sql, params) { rs -> mapPersonRow(rs) }
            }

        val hasNext = rows.size > limit
        val page = if (hasNext) rows.dropLast(1) else rows
        val keyset = page.lastOrNull()?.let { PersonKeyset(it.name, it.id) }
        return PersonPage(page, keyset, hasNext)
    }

    fun findPerson(id: UUID): PersonRow? =
        dataSource.connection.use { conn ->
            conn
                .queryBound(
                    "SELECT id, name, biography, status, version FROM person WHERE id = ? AND status = 'ACTIVE'",
                    listOf(id),
                ) { rs -> mapPersonRow(rs) }
                .firstOrNull()
        }

    fun titleCredits(
        titleId: UUID,
        first: Int,
        after: CreditKeyset?,
    ): CreditPage =
        fetchCredits(
            where =
                buildList {
                    add("t.status = 'ACTIVE'")
                    add("p.status = 'ACTIVE'")
                    add("c.title_id = ?")
                },
            params =
                buildList {
                    add(titleId)
                    after?.let { add(it.id) }
                },
            after = after,
            first = first,
        )

    fun personCredits(
        personId: UUID,
        first: Int,
        after: CreditKeyset?,
    ): CreditPage =
        fetchCredits(
            where =
                buildList {
                    add("t.status = 'ACTIVE'")
                    add("p.status = 'ACTIVE'")
                    add("c.person_id = ?")
                },
            params =
                buildList {
                    add(personId)
                    after?.let { add(it.id) }
                },
            after = after,
            first = first,
        )

    private fun fetchCredits(
        where: List<String>,
        params: List<Any?>,
        after: CreditKeyset?,
        first: Int,
    ): CreditPage {
        val limit = ReadPolicy(policy()).pageSize(first)
        val mutableWhere = where.toMutableList()
        val mutableParams = params.toMutableList()
        if (after != null) {
            mutableWhere.add("c.id > ?")
        }
        val sql = """SELECT c.id, c.title_id, c.person_id, c.role_id, c.characters, c.version,
                            p.name AS person_name, t.display_title AS title_name,
                            r.category AS role_category, r.label AS role_label,
                            r.allows_characters AS role_allows_characters
                     FROM credit c
                     JOIN title t ON t.id = c.title_id
                     JOIN person p ON p.id = c.person_id
                     JOIN credit_role r ON r.id = c.role_id
                     WHERE ${mutableWhere.joinToString(" AND ")}
                     ORDER BY c.id ASC
                     LIMIT ${limit + 1}"""
        val rows =
            dataSource.connection.use { conn ->
                conn.queryBound(sql, mutableParams) { rs -> mapCreditRow(rs) }
            }
        val hasNext = rows.size > limit
        val page = if (hasNext) rows.dropLast(1) else rows
        val keyset = page.lastOrNull()?.let { CreditKeyset(it.id) }
        return CreditPage(page, keyset, hasNext)
    }

    private fun mapPersonRow(rs: ResultSet) =
        PersonRow(
            id = rs.getObject("id", UUID::class.java),
            name = rs.getString("name"),
            biography = rs.getString("biography"),
            status = rs.getString("status"),
            version = rs.getLong("version"),
        )

    private fun mapCreditRow(rs: ResultSet): CreditRow {
        val rawChars = rs.getArray("characters")?.array
        val characters = (rawChars as? Array<*>)?.map { it.toString() } ?: emptyList()
        return CreditRow(
            id = rs.getObject("id", UUID::class.java),
            titleId = rs.getObject("title_id", UUID::class.java),
            personId = rs.getObject("person_id", UUID::class.java),
            roleId = rs.getObject("role_id", UUID::class.java),
            characters = characters,
            version = rs.getLong("version"),
            personName = rs.getString("person_name"),
            titleName = rs.getString("title_name"),
            roleCategory = rs.getString("role_category"),
            roleLabel = rs.getString("role_label"),
            roleAllowsCharacters = rs.getBoolean("role_allows_characters"),
        )
    }


}
