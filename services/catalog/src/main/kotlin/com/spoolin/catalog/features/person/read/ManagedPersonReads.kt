package com.spoolin.catalog.features.person.read

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
import spoolin.v1.Spoolin.Id
import spoolin.v1.Spoolin.PageRequest
import spoolin.v1.Spoolin.Person
import spoolin.v1.Spoolin.PersonPage
import spoolin.v1.Spoolin.PersonValues
import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource


/**
 * Managed (Maintainer-only) person read operations for the Catalog gRPC service.
 *
 * ManagedPerson: returns ACTIVE or ARCHIVED person by id (private projection).
 * ManagedPeople: returns people matching explicit ACTIVE|ARCHIVED state, name/UUID order.
 *
 * F-PERMISSIONS: MAINTAINER role required; enforced before every call.
 * ATDD-251-T1-R12: managedPerson — private active/archived projection.
 * ATDD-251-T1-R15: managedPeople — explicit state, name/UUID order.
 * ATDD-253-T1-R11: ManagedPeople(PageRequest) → PersonPage.
 * ATDD-253-T1-R13: ManagedPerson(Id) → Person.
 */
class ManagedPersonReads(
    private val dataSource: DataSource,
    private val policy: () -> PublicPolicy = { PolicyEnvironment.resolve(System::getenv) },
    private val cursors: ManagedReadCursors = ManagedReadCursors.ephemeral(),
) {
    fun managedPerson(
        request: Id,
        response: StreamObserver<Person>,
    ) {
        try {
            requireMaintainer()
            val id = parseId(request.id)
            val row = findManagedPerson(id)
            if (row == null) {
                response.onNext(Person.getDefaultInstance())
                response.onCompleted()
                return
            }
            response.onNext(personProto(row))
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        } catch (_: InputViolation) {
            response.onError(Status.INVALID_ARGUMENT.withDescription("FIELD_INVALID").asRuntimeException())
        }
    }

    fun managedPeople(
        request: PageRequest,
        response: StreamObserver<PersonPage>,
    ) {
        try {
            requireMaintainer()
            val state =
                request.state.takeIf { it.isNotEmpty() }
                    ?: throw Status.INVALID_ARGUMENT.withDescription("INVALID_STATE").asRuntimeException()
            if (state !in setOf("ACTIVE", "ARCHIVED")) {
                throw Status.INVALID_ARGUMENT.withDescription("INVALID_STATE").asRuntimeException()
            }
            val first = ReadPolicy(policy()).rpcPageSize(request.first)
            val scope = cursors.scope(ReadPolicy(policy()), "managed-people", first, mapOf("state" to state))
            val after = cursors.person(request.after.takeIf { it.isNotEmpty() }, scope)
            val page = listManagedPeople(state, first, after, scope)
            val builder = PersonPage.newBuilder()
            page.rows.forEach { builder.addNodes(personProto(it)) }
            page.endCursor?.let { builder.endCursor = it }
            builder.hasNextPage = page.hasNextPage
            response.onNext(builder.build())
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        } catch (_: InputViolation) {
            response.onError(Status.INVALID_ARGUMENT.withDescription("FIELD_INVALID").asRuntimeException())
        }
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private fun requireMaintainer() {
        val claims = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
        if (!Permissions.management(claims.role)) throw ActorRejected()
    }

    private data class ManagedPersonRow(
        val id: UUID,
        val name: String,
        val biography: String?,
        val status: String,
        val version: Long,
    )

    private data class ManagedPersonPage(
        val rows: List<ManagedPersonRow>,
        val endCursor: String?,
        val hasNextPage: Boolean,
    )

    private fun findManagedPerson(id: UUID): ManagedPersonRow? =
        dataSource.connection.use { conn ->
            conn
                .queryBound(
                    "SELECT id, name, biography, status, version FROM person WHERE id = ?",
                    listOf(id),
                ) { mapRow(it) }
                .firstOrNull()
        }

    private fun listManagedPeople(
        state: String,
        first: Int,
        after: com.spoolin.catalog.features.search.PeopleCursor.Keyset?,
        scope: String,
    ): ManagedPersonPage {
        val params = mutableListOf<Any?>(state)
        var keysetClause = ""
        val cursor = after
        if (cursor != null) {
            keysetClause = " AND (p.name > ? OR (p.name = ? AND p.id > ?))"
            params.add(cursor.name)
            params.add(cursor.name)
            params.add(cursor.id)
        }
        val sql =
            "SELECT id, name, biography, status, version FROM person p" +
                " WHERE p.status = ?$keysetClause" +
                " ORDER BY p.name ASC, p.id ASC" +
                " LIMIT ${first + 1}"
        val rows =
            dataSource.connection.use { conn ->
                conn.queryBound(sql, params) { mapRow(it) }
            }
        val hasNext = rows.size > first
        val page = if (hasNext) rows.dropLast(1) else rows
        val endCursor = page.lastOrNull()?.let {
            cursors.people.encode(com.spoolin.catalog.features.search.PeopleCursor.Keyset(it.name, it.id),
                scope, cursors.now())
        }
        return ManagedPersonPage(page, endCursor, hasNext)
    }

    private fun mapRow(rs: ResultSet) =
        ManagedPersonRow(
            id = rs.getObject("id", UUID::class.java),
            name = rs.getString("name"),
            biography = rs.getString("biography"),
            status = rs.getString("status"),
            version = rs.getLong("version"),
        )

    private fun personProto(row: ManagedPersonRow): Person =
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

    private fun parseId(value: String): UUID =
        runCatching { UUID.fromString(value).also { require(it.toString() == value) } }
            .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("INVALID_ID").asRuntimeException() }
}
