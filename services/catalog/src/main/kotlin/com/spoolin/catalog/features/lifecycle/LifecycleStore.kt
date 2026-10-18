package com.spoolin.catalog.features.lifecycle

import com.spoolin.catalog.features.artwork.ArtworkRpc
import com.spoolin.catalog.operations.audit.AuditInsert
import com.spoolin.support.receipts.SafeResult
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import io.grpc.Status
import java.sql.Connection
import java.time.Clock
import java.util.UUID

internal class LifecycleStore(private val clock: Clock = Clock.systemUTC()) {
    fun movie(connection: Connection, command: LifecycleCommand): SafeResult {
        val title = connection.queryBound("SELECT kind,status,version,attachment_id FROM title WHERE id=? FOR UPDATE",
            listOf(command.target)) { row ->
            TitleState(row.getString("kind"), row.getString("status"), row.getLong("version"),
                row.getObject("attachment_id", UUID::class.java))
        }.singleOrNull() ?: throw Status.NOT_FOUND.asRuntimeException()
        if (title.kind != "MOVIE") throw Status.PERMISSION_DENIED.withDescription("TV_READ_ONLY").asRuntimeException()
        val next = nextState(title.status, title.version, command)
        lockTitlePeople(connection, command.target)
        check(connection.executeBound("UPDATE title SET status=?,version=version+1,updated_at=CURRENT_TIMESTAMP " +
            "WHERE id=? AND version=? AND kind='MOVIE'", next, command.target, command.expectedVersion) == 1)
        if (command.action == LifecycleRules.Action.DELETE) deleteMovieContents(connection, command, title.hold)
        audit(connection, command, "TITLE", title.status, next)
        return SafeResult(command.target, command.expectedVersion + 1)
    }

    private fun lockTitlePeople(connection: Connection, target: UUID) {
        connection.queryBound("SELECT p.id FROM person p WHERE EXISTS " +
            "(SELECT 1 FROM credit c WHERE c.person_id=p.id AND c.title_id=?) ORDER BY p.id FOR UPDATE",
            listOf(target)) { it.getObject("id", UUID::class.java) }
        connection.executeBound("UPDATE person SET impact_version=impact_version+1,updated_at=CURRENT_TIMESTAMP " +
            "WHERE id IN (SELECT person_id FROM credit WHERE title_id=?)", target)
    }

    private fun deleteMovieContents(connection: Connection, command: LifecycleCommand, hold: UUID?) {
        removeCreditTombstones(connection, "title_id", command.target)
        connection.executeBound("DELETE FROM credit WHERE title_id=?", command.target)
        connection.executeBound("DELETE FROM title_genre WHERE title_id=?", command.target)
        tombstone(connection, "TITLE", command.target)
        connection.executeBound("UPDATE title SET asset_id=NULL,attachment_id=NULL WHERE id=?", command.target)
        if (hold != null) ArtworkRpc.enqueueRelease(connection, command.operation, hold, command.correlation)
    }

    fun nextState(status: String, version: Long, command: LifecycleCommand): String {
        if (version != command.expectedVersion) throw Status.ABORTED.withDescription("STALE_VERSION").asRuntimeException()
        if (command.action !in LifecycleRules.allowedActions(status)) throw Status.FAILED_PRECONDITION
            .withDescription("LIFECYCLE_STATE_CHANGED").asRuntimeException()
        if (command.action == LifecycleRules.Action.DELETE && !command.confirmed) throw Status.FAILED_PRECONDITION
            .withDescription("CONFIRMATION_REQUIRED").asRuntimeException()
        return when (command.action) {
            LifecycleRules.Action.ARCHIVE -> "ARCHIVED"
            LifecycleRules.Action.RESTORE -> "ACTIVE"
            LifecycleRules.Action.DELETE -> "DELETED"
        }
    }

    fun removeCreditTombstones(connection: Connection, ownerColumn: String, target: UUID) {
        require(ownerColumn in setOf("title_id", "person_id"))
        connection.executeBound("UPDATE seed_member SET removed_at=COALESCE(removed_at,CURRENT_TIMESTAMP) " +
            "WHERE subject_kind='CREDIT' AND subject_id IN (SELECT id FROM credit WHERE $ownerColumn=?)", target)
    }

    fun tombstone(connection: Connection, kind: String, target: UUID) {
        require(kind in setOf("TITLE", "PERSON"))
        connection.executeBound("UPDATE seed_member SET removed_at=COALESCE(removed_at,CURRENT_TIMESTAMP) " +
            "WHERE subject_kind=? AND subject_id=?", kind, target)
        connection.executeBound("UPDATE provenance_record SET deleted_at=COALESCE(deleted_at,CURRENT_TIMESTAMP) " +
            "WHERE subject_kind=? AND subject_id=?", kind, target)
    }

    fun audit(connection: Connection, command: LifecycleCommand, kind: String, before: String, after: String) {
        AuditInsert.record(connection, command.operation, command.actor, "HUMAN", "${command.action}_${if (kind == "TITLE") "MOVIE" else "PERSON"}",
            kind, command.target, "SUCCEEDED",
            "{\"changes\":[{\"path\":\"status\",\"before\":\"$before\",\"after\":\"$after\"}],\"summarised\":false}",
            command.correlation, clock.instant())
    }

    private data class TitleState(val kind: String, val status: String, val version: Long, val hold: UUID?)
}
