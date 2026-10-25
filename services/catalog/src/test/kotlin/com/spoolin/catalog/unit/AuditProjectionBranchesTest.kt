package com.spoolin.catalog.unit

import com.spoolin.catalog.features.movie.ManagedMovieReads
import com.spoolin.catalog.features.provenance.ProvenanceReads
import com.spoolin.catalog.features.search.TitleSearchSql
import com.spoolin.catalog.operations.audit.AuditReads
import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin
import java.sql.Timestamp

class AuditProjectionBranchesTest {
    private fun read(json: String, kind: String = "TITLE", actorKind: String = "HUMAN", actor: java.util.UUID? = unitActor): RpcResult<Spoolin.AuditPage> {
        val row = mapOf("id" to unitOperation, "actor_id" to actor, "actor_kind" to actorKind,
            "action" to "UPDATE", "subject_kind" to kind, "subject_id" to null, "outcome" to "SUCCEEDED", "changes" to json,
            "operation_id" to null, "correlation_id" to unitCorrelation, "created_at" to Timestamp.from(unitNow))
        val db = JdbcRows(listOf(row))
        val result = RpcResult<Spoolin.AuditPage>()
        withMaintainer {
            ManagedMovieReads(db.source, ProvenanceReads(db.source), TitleSearchSql(db.source), AuditReads(db.source))
                .audit(Spoolin.PageRequest.newBuilder().setFirst(10).build(), result)
        }
        return result
    }

    @Test
    fun `legacy audit values retain only known typed domain fields`() {
        val result = read("""{"displayTitle":{"before":null,"after":"New"},"countries":{"before":["US"],"after":[]},"version":{"before":1,"after":2},"secret":{"before":"private","after":"private"}}""").value()
        assertEquals(listOf("countries", "displayTitle", "version"), result.nodesList.single().changesList.map { it.path })
        assertEquals("null", result.nodesList.single().changesList[1].beforeJson)
        assertFalse(result.nodesList.single().hasSubjectId())
        assertFalse(result.nodesList.single().hasOperationId())
        assertEquals("UNKNOWN", read("{}", actor = null).value().nodesList.single().actorRef)
        assertEquals("SERVICE", read("{}", actorKind = "SERVICE").value().nodesList.single().actorRef)
    }

    @Test
    fun `audit supports person credit role and unknown subject kinds without exposing unsupported paths`() {
        for ((kind, path, value) in listOf(Triple("PERSON", "biography", "\"Bio\""), Triple("CREDIT", "credit", "\"present\""),
            Triple("ROLE", "allowsCharacters", "true"), Triple("ROLE", "category", "\"CAST\""))) {
            val result = read("""{"changes":[{"path":"$path","after":$value},{"path":"private","after":"hidden"}]}""", kind).value()
            assertEquals(listOf(path), result.nodesList.single().changesList.map { it.path })
        }
        assertEquals(0, read("""{"changes":[{"path":"title","after":"hidden"}]}""", "UNKNOWN").value().nodesList.single().changesCount)
    }

    @Test
    fun `omitted audit values expose bounded digests and byte count only`() {
        val digest = "a".repeat(64)
        for (hash in listOf("null", "\"$digest\"")) {
            val result = read("""{"changes":[{"path":"synopsis","valueOmitted":true,"byteLength":123,"beforeDigest":$hash,"afterDigest":$hash}]}""").value()
            val change = result.nodesList.single().changesList.single()
            assertFalse(change.hasBeforeJson())
            assertEquals(true, change.afterJson.contains("123"))
        }
    }

    @Test
    fun `malformed private or wrongly typed audit representation fails with data loss`() {
        val invalid = listOf("[]", "{bad", """{"changes":{}}""", """{"changes":[],"truncated":true}""",
            """{"displayTitle":3}""", """{"version":{"after":"string"}}""", """{"countries":{"after":[1]}}""",
            """{"changes":[{"path":"synopsis","valueOmitted":true,"byteLength":-1,"beforeDigest":null,"afterDigest":null}]}""",
            """{"changes":[{"path":"synopsis","valueOmitted":true,"byteLength":1,"beforeDigest":"wrong","afterDigest":null}]}""",
            """{"changes":[{"path":"synopsis","valueOmitted":true,"byteLength":"one","beforeDigest":null,"afterDigest":null}]}""",
            " ".repeat(65537))
        for (json in invalid) assertEquals(Status.Code.DATA_LOSS, Status.fromThrowable(read(json).failure).code)
    }
}
