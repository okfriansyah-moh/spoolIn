@file:Suppress("CyclomaticComplexMethod", "ThrowsCount")

package com.spoolin.catalog.features.credit

import com.spoolin.support.policy.PublicPolicy
import com.spoolin.support.policy.PolicyEnvironment
import com.spoolin.support.policy.ReadPolicy
import com.spoolin.support.policy.InputViolation
import com.spoolin.catalog.features.search.ManagedReadCursors
import com.spoolin.support.identity.ActorRejected
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.Permissions
import com.spoolin.support.receipts.queryBound
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.Spoolin.Credit
import spoolin.v1.Spoolin.CreditPage
import spoolin.v1.Spoolin.CreditRole
import spoolin.v1.Spoolin.PageRequest
import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource


/**
 * Maintainer managed credit reads — includes retained archived-person credits on active movies.
 */
class ManagedCreditReads(
    private val dataSource: DataSource,
    private val policy: () -> PublicPolicy = { PolicyEnvironment.resolve(System::getenv) },
    private val cursors: ManagedReadCursors = ManagedReadCursors.ephemeral(),
) {
    fun managedCredits(
        request: PageRequest,
        response: StreamObserver<CreditPage>,
    ) {
        try {
            requireMaintainer()
            val kind =
                request.targetKind.takeIf { it.isNotEmpty() }
                    ?: throw Status.INVALID_ARGUMENT.withDescription("INVALID_SELECTOR").asRuntimeException()
            val targetId =
                request.targetId.takeIf { it.isNotEmpty() }?.let { parseId(it) }
                    ?: throw Status.INVALID_ARGUMENT.withDescription("INVALID_SELECTOR").asRuntimeException()
            val titleId = if (kind == "TITLE") targetId else null
            val personId = if (kind == "PERSON") targetId else null
            if (titleId == null && personId == null) {
                throw Status.INVALID_ARGUMENT.withDescription("INVALID_SELECTOR").asRuntimeException()
            }
            val first = ReadPolicy(policy()).rpcPageSize(request.first)
            val scope = cursors.scope(ReadPolicy(policy()), "managed-credits", first,
                mapOf("kind" to kind, "target" to targetId.toString()))
            val after = cursors.title(request.after.takeIf { it.isNotEmpty() }, scope)?.id
            val page = listCredits(titleId, personId, first, after)
            val builder = CreditPage.newBuilder()
            page.rows.forEach { builder.addNodes(creditProto(it)) }
            page.endCursor?.let {
                builder.endCursor = cursors.titles.encode(
                    com.spoolin.catalog.features.search.CatalogCursor.Keyset(null, null, null, it), scope, cursors.now(),
                )
            }
            builder.hasNextPage = page.hasNextPage
            response.onNext(builder.build())
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        } catch (_: InputViolation) {
            response.onError(Status.INVALID_ARGUMENT.withDescription("FIELD_INVALID").asRuntimeException())
        }
    }

    private data class CreditRow(
        val id: UUID,
        val titleId: UUID,
        val personId: UUID,
        val roleId: UUID,
        val characters: List<String>,
        val version: Long,
        val personName: String,
        val personStatus: String,
        val titleName: String,
        val roleCategory: String,
        val roleLabel: String,
        val roleAllowsCharacters: Boolean,
    )

    private data class CreditPageResult(
        val rows: List<CreditRow>,
        val endCursor: UUID?,
        val hasNextPage: Boolean,
    )

    private fun listCredits(
        titleId: UUID?,
        personId: UUID?,
        first: Int,
        after: UUID?,
    ): CreditPageResult {
        val params = mutableListOf<Any?>()
        val where = mutableListOf("t.status = 'ACTIVE'")
        if (titleId != null) {
            where.add("c.title_id = ?")
            params.add(titleId)
        }
        if (personId != null) {
            where.add("c.person_id = ?")
            params.add(personId)
        }
        if (after != null) {
            where.add("c.id > ?")
            params.add(after)
        }
        val sql =
            """
            SELECT c.id, c.title_id, c.person_id, c.role_id, c.characters, c.version,
                   p.name AS person_name, p.status AS person_status, t.display_title AS title_name,
                   r.category AS role_category, r.label AS role_label, r.allows_characters AS role_allows_characters
            FROM credit c
            JOIN title t ON t.id = c.title_id
            JOIN person p ON p.id = c.person_id
            JOIN credit_role r ON r.id = c.role_id
            WHERE ${where.joinToString(" AND ")}
            ORDER BY c.id ASC
            LIMIT ${first + 1}
            """.trimIndent()
        val rows =
            dataSource.connection.use { conn ->
                conn.queryBound(sql, params) { rs -> mapRow(rs) }
            }
        val hasNext = rows.size > first
        val page = if (hasNext) rows.dropLast(1) else rows
        return CreditPageResult(page, page.lastOrNull()?.id, hasNext)
    }

    private fun mapRow(rs: ResultSet): CreditRow {
        val raw = rs.getArray("characters")?.array
        val characters = (raw as? Array<*>)?.map { it.toString() } ?: emptyList()
        return CreditRow(
            id = rs.getObject("id", UUID::class.java),
            titleId = rs.getObject("title_id", UUID::class.java),
            personId = rs.getObject("person_id", UUID::class.java),
            roleId = rs.getObject("role_id", UUID::class.java),
            characters = characters,
            version = rs.getLong("version"),
            personName = rs.getString("person_name"),
            personStatus = rs.getString("person_status"),
            titleName = rs.getString("title_name"),
            roleCategory = rs.getString("role_category"),
            roleLabel = rs.getString("role_label"),
            roleAllowsCharacters = rs.getBoolean("role_allows_characters"),
        )
    }

    private fun creditProto(row: CreditRow): Credit =
        Credit
            .newBuilder()
            .setId(row.id.toString())
            .setTitleId(row.titleId.toString())
            .setPersonId(row.personId.toString())
            .setRoleId(row.roleId.toString())
            .addAllCharacters(row.characters)
            .setVersion(row.version)
            .setPersonName(row.personName)
            .setTitleName(row.titleName)
            .setRole(
                CreditRole
                    .newBuilder()
                    .setId(row.roleId.toString())
                    .setCategory(row.roleCategory)
                    .setLabel(row.roleLabel)
                    .setAllowsCharacters(row.roleAllowsCharacters)
                    .build(),
            ).build()

    private fun requireMaintainer() {
        val claims = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
        if (!Permissions.management(claims.role)) throw ActorRejected()
    }

    private fun parseId(value: String): UUID =
        runCatching { UUID.fromString(value).also { require(it.toString() == value) } }
            .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("INVALID_ID").asRuntimeException() }
}
