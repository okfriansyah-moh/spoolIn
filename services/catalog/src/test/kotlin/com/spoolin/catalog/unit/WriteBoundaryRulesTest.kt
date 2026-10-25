package com.spoolin.catalog.unit

import com.google.protobuf.FieldMask
import com.spoolin.catalog.boundary.policy.CatalogWriteIntent
import com.spoolin.catalog.boundary.policy.CatalogWritePolicy
import com.spoolin.catalog.features.credit.CreditWrites
import com.spoolin.catalog.features.movie.MovieWrites
import com.spoolin.catalog.features.person.write.PersonWrites
import com.spoolin.catalog.operations.receipts.OwnerReceipts
import com.spoolin.support.receipts.IntentHashes
import com.spoolin.support.receipts.OperationIntent
import io.grpc.Status
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class WriteBoundaryRulesTest {
    private val id = UUID.fromString("00000000-0000-4000-8000-000000000011")
    private val other = UUID.fromString("00000000-0000-4000-8000-000000000012")
    private val operation = UUID.fromString("00000000-0000-4000-8000-000000000013")
    private val actor = UUID.fromString("00000000-0000-4000-8000-000000000001")
    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val policy = CatalogWritePolicy.environment()
    private val hashes = IntentHashes(mapOf("test" to ByteArray(32) { 1 }), "test")
    private val context = Spoolin.Context.newBuilder().setOperationId(operation.toString())
        .setCorrelationId(other.toString()).setPolicyRevision(policy.revision).build()

    private fun database(kind: String, fields: List<Pair<String, String?>>, vararg domain: List<Map<String, Any?>>, version: Long = 8): JdbcRows {
        val intent = OperationIntent(operation, "account:$actor", kind, policy.revision, fields)
        val receipt = mapOf(
            "id" to operation, "receipt_type" to "REQUEST", "state" to "PENDING", "expires_at" to Timestamp.from(now.plusSeconds(86400)),
            "created_at" to Timestamp.from(now), "correlation_id" to other, "actor_scope" to "account:$actor", "kind" to kind,
            "request_hash" to hashes.hash(intent, "test"), "hash_key_id" to "test", "result" to null, "error_code" to null,
        )
        val complete = receipt + mapOf("state" to "SUCCEEDED", "result" to """{"resultId":"$id","version":"$version"}""")
        return JdbcRows(listOf(mapOf("lock" to 1)), listOf(receipt), *domain, listOf(complete))
    }
    private fun owner(db: JdbcRows) = OwnerReceipts(db.source, hashes, clock)
    private fun mask(vararg paths: String) = FieldMask.newBuilder().addAllPaths(paths.toList()).build()
    private fun personRequest() = Spoolin.PersonWrite.newBuilder().setContext(context)
        .setValues(Spoolin.PersonValues.newBuilder().setName(" Ada ").setBiography(" Biography "))
    private fun personRow(status: String = "ACTIVE", version: Long = 7) = listOf(mapOf(
        "name" to "Ada", "biography" to "Biography", "status" to status, "version" to version,
    ))
    private fun titleRow(status: String = "ACTIVE", kind: String = "MOVIE", version: Long = 7) = listOf(mapOf(
        "kind" to kind, "status" to status, "version" to version, "display_title" to "River",
        "original_title" to "Old River", "synopsis" to "A journey", "synopsis_language" to "en",
        "original_language" to "fr", "countries" to arrayOf("FR"), "release_year" to 2024,
        "release_month" to 2, "release_day" to 29, "runtime_minutes" to 95,
    ))
    private fun movieRequest() = Spoolin.MovieWrite.newBuilder().setContext(context).setValues(
        Spoolin.MovieValues.newBuilder().setTitle(" River ").setSynopsis(" A journey ")
            .setSynopsisLanguage("en").setOriginalTitle(" Original ").setOriginalLanguage("fr")
            .addCountries("FR").addGenreIds(other.toString()).setRuntimeMinutes(95)
            .setRelease(Spoolin.PartialDate.newBuilder().setYear(2024).setMonth(2).setDay(29)),
    )

    @Test fun `person creation writes normalized plain text and a success audit`() = withMaintainer {
        val request = personRequest().build()
        val db = database("WritePerson", CatalogWriteIntent.person(request), version = 0)
        val result = RpcResult<Spoolin.Outcome>()
        PersonWrites(owner(db), clock).writePerson(request, result)
        assertEquals("SUCCEEDED", result.value().state)
        assertEquals(0L, result.value().version)
        assertTrue(db.writes.any { "Ada" in it && "Biography" in it })
        assertTrue(db.writes.flatten().filterIsInstance<String>().any { it.contains("Biography") && it.contains("changes") })
    }

    @Test fun `person patch supports biography replacement clearing and omission`() = withMaintainer {
        for (biography in listOf("New biography", "", null)) {
            val values = Spoolin.PersonValues.newBuilder().setName("New name")
            if (biography != null) values.biography = biography
            val request = personRequest().setId(id.toString()).setExpectedVersion(7).setValues(values)
                .setUpdateMask(if (biography == null) mask("values.name") else mask("values.name", "values.biography")).build()
            val db = database("WritePerson", CatalogWriteIntent.person(request), personRow())
            val result = RpcResult<Spoolin.Outcome>()
            PersonWrites(owner(db), clock).writePerson(request, result)
            assertEquals(8L, result.value().version)
            assertTrue(db.writes.any { "New name" in it && 8L in it })
            val changes = db.writes.flatten().filterIsInstance<String>().first { it.contains("changes") }
            assertTrue(changes.contains("New name"))
            if (biography == "New biography") assertTrue(changes.contains("New biography"))
            if (biography == "") assertTrue(changes.contains("null"))
        }
    }

    @Test fun `person update rejects absent archived hidden and stale identities`() = withMaintainer {
        for ((rows, code) in listOf(
            emptyList<Map<String, Any?>>() to "NOT_FOUND", personRow("ARCHIVED") to "PERSON_ARCHIVED",
            personRow("DELETED") to "NOT_FOUND", personRow(version = 6) to "VERSION_CONFLICT",
        )) {
            val request = personRequest().setId(id.toString()).setExpectedVersion(7).build()
            val db = database("WritePerson", CatalogWriteIntent.person(request), rows)
            val result = RpcResult<Spoolin.Outcome>()
            PersonWrites(owner(db), clock).writePerson(request, result)
            assertEquals(code, Status.fromThrowable(result.failure).description)
            assertEquals(1, db.writes.size, "Only receipt reservation may precede rejected mutation")
        }
    }

    @Test fun `person input errors return field problem rather than successful receipt`() = withMaintainer {
        for (update in listOf(false, true)) {
            val builder = personRequest().setValues(Spoolin.PersonValues.newBuilder().setName(" "))
            if (update) builder.id = id.toString()
            val request = builder.build()
            val db = database("WritePerson", CatalogWriteIntent.person(request))
            val result = RpcResult<Spoolin.Outcome>()
            PersonWrites(owner(db), clock).writePerson(request, result)
            assertEquals("FAILED", result.value().state)
            assertEquals("FIELD_INVALID", result.value().problem.code)
            assertEquals(if (update) "expectedVersion" else "name", result.value().problem.fieldsList.single().path)
        }
    }

    @Test fun `movie create writes supplied date precision genres and normalized text`() = withMaintainer {
        val request = movieRequest().build()
        val db = database("WriteMovie", CatalogWriteIntent.movie(request), listOf(mapOf("id" to other)), version = 0)
        val result = RpcResult<Spoolin.Outcome>()
        MovieWrites(owner(db), clock).writeMovie(request, result)
        assertEquals("SUCCEEDED", result.value().state)
        assertTrue(db.writes.any { "River" in it && "A journey" in it && 2024 in it && 29 in it })
        assertTrue(db.writes.any { other in it && it.size == 2 })
    }

    @Test fun `movie patch replaces all supplied fields and records incremented version`() = withMaintainer {
        val request = movieRequest().setId(id.toString()).setExpectedVersion(7).setUpdateMask(mask(
            "values.title", "values.original_title", "values.synopsis", "values.synopsis_language",
            "values.original_language", "values.countries", "values.genre_ids", "values.release",
            "values.runtime_minutes",
        )).build()
        val genres = listOf(mapOf("genre_id" to other))
        val known = listOf(mapOf("id" to other))
        val db = database("WriteMovie", CatalogWriteIntent.movie(request), titleRow(), genres, known, known)
        val result = RpcResult<Spoolin.Outcome>()
        MovieWrites(owner(db), clock).writeMovie(request, result)
        assertEquals(8L, result.value().version)
        assertTrue(db.writes.any { "River" in it && "Original" in it && 8L in it })
    }

    @Test fun `movie optional fields can be cleared without changing omitted required text`() = withMaintainer {
        val request = Spoolin.MovieWrite.newBuilder().setContext(context).setId(id.toString()).setExpectedVersion(7)
            .setUpdateMask(mask("values.original_title", "values.original_language", "values.runtime_minutes", "values.release"))
            .setValues(Spoolin.MovieValues.newBuilder().setRelease(Spoolin.PartialDate.newBuilder().setYear(2024))).build()
        val db = database("WriteMovie", CatalogWriteIntent.movie(request), titleRow(), listOf(mapOf("genre_id" to other)))
        val result = RpcResult<Spoolin.Outcome>()
        MovieWrites(owner(db), clock).writeMovie(request, result)
        assertEquals(8L, result.value().version)
        assertTrue(db.writes.flatten().filterIsInstance<String>().any { it.contains("null") && it.contains("changes") })
    }

    @Test fun `movie update rejects absent television archived and stale rows`() = withMaintainer {
        for ((rows, code) in listOf(
            emptyList<Map<String, Any?>>() to "NOT_FOUND", titleRow(kind = "TV") to "TV_WRITE_DENIED",
            titleRow(status = "ARCHIVED") to "NOT_FOUND", titleRow(version = 5) to "VERSION_CONFLICT",
        )) {
            val request = movieRequest().setId(id.toString()).setExpectedVersion(7).build()
            val db = database("WriteMovie", CatalogWriteIntent.movie(request), rows)
            val result = RpcResult<Spoolin.Outcome>()
            MovieWrites(owner(db), clock).writeMovie(request, result)
            assertEquals(code, Status.fromThrowable(result.failure).description)
            assertEquals(1, db.writes.size)
        }
    }

    @Test fun `unknown genre returns a field issue without persisting movie`() = withMaintainer {
        val request = movieRequest().build()
        val db = database("WriteMovie", CatalogWriteIntent.movie(request), emptyList())
        val result = RpcResult<Spoolin.Outcome>()
        MovieWrites(owner(db), clock).writeMovie(request, result)
        assertEquals("genreIds", result.value().problem.fieldsList.single().path)
        assertEquals(1, db.writes.size)
    }

    @Test fun `write entrypoints require actor and valid operation identifiers`() {
        val result = RpcResult<Spoolin.Outcome>()
        val receipts = owner(JdbcRows())
        MovieWrites(receipts, clock).writeMovie(movieRequest().build(), result)
        assertEquals(Status.Code.PERMISSION_DENIED, Status.fromThrowable(result.failure).code)
        withMaintainer {
            val invalid = RpcResult<Spoolin.Outcome>()
            PersonWrites(receipts, clock).writePerson(personRequest().setContext(context.toBuilder().setOperationId("bad")).build(), invalid)
            assertEquals("INVALID_ID", Status.fromThrowable(invalid.failure).description)
        }
    }
    private fun creditRequest() = Spoolin.CreditWrite.newBuilder().setContext(context)
        .setTitleId(id.toString()).setPersonId(other.toString()).setRoleId(other.toString())
        .setExpectedVersion(7).addCharacters(" Alice ")
    private fun creditRow(version: Long = 7) = listOf(mapOf(
        "title_id" to id, "person_id" to other, "role_id" to other, "characters" to arrayOf("Alice"), "version" to version,
    ))
    private fun role() = listOf(mapOf("category" to "CAST", "allows_characters" to true))

    @Test fun `credit creation normalizes characters and advances related title`() = withMaintainer {
        val request = creditRequest().build()
        val db = database("WriteCredit", CatalogWriteIntent.credit(request), titleRow(),
            listOf(mapOf("status" to "ACTIVE")), role(), emptyList(), version = 0)
        val result = RpcResult<Spoolin.Outcome>()
        CreditWrites(owner(db), clock).writeCredit(request, result)
        assertEquals("SUCCEEDED", result.value().state)
        assertEquals(0L, result.value().version)
        assertTrue(db.writes.any { 8L in it && id in it })
        assertTrue(db.writes.flatten().filterIsInstance<String>().any { it.contains("changes") })
    }

    @Test fun `credit patch preserves omitted role and supports replacement characters`() = withMaintainer {
        for (paths in listOf(emptyArray<String>(), arrayOf("roleId", "characters"))) {
            val request = creditRequest().setId(id.toString()).setUpdateMask(mask(*paths)).build()
            val db = database("WriteCredit", CatalogWriteIntent.credit(request), creditRow(), titleRow(),
                listOf(mapOf("status" to "ACTIVE")), role())
            val result = RpcResult<Spoolin.Outcome>()
            CreditWrites(owner(db), clock).writeCredit(request, result)
            assertEquals(8L, result.value().version)
            assertTrue(db.writes.any { id in it && 8L in it && other in it })
        }
    }

    @Test fun `credit removal remains allowed for archived people`() = withMaintainer {
        val request = Spoolin.VersionedId.newBuilder().setContext(context).setId(id.toString()).setExpectedVersion(7).build()
        val db = database("RemoveCredit", CatalogWriteIntent.remove(request), creditRow(), titleRow(),
            listOf(mapOf("status" to "ARCHIVED")))
        val result = RpcResult<Spoolin.Outcome>()
        CreditWrites(owner(db), clock).removeCredit(request, result)
        assertEquals(8L, result.value().version)
        assertTrue(db.writes.any { it == listOf(id, 7L) })
        assertTrue(db.writes.flatten().filterIsInstance<String>().any { it.contains("present") && it.contains("null") })
    }

    @Test fun `credit create rejects archived television and stale titles before mutation`() = withMaintainer {
        for ((row, code) in listOf(
            emptyList<Map<String, Any?>>() to "NOT_FOUND", titleRow(status = "ARCHIVED") to "TITLE_ARCHIVED",
            titleRow(kind = "TV") to "TV_CREDIT_DENIED", titleRow(version = 5) to "VERSION_CONFLICT",
        )) {
            val request = creditRequest().build()
            val db = database("WriteCredit", CatalogWriteIntent.credit(request), row)
            val result = RpcResult<Spoolin.Outcome>()
            CreditWrites(owner(db), clock).writeCredit(request, result)
            assertEquals(code, Status.fromThrowable(result.failure).description)
            assertEquals(1, db.writes.size)
        }
    }

    @Test fun `credit create rejects archived absent people unknown roles and duplicates`() = withMaintainer {
        val request = creditRequest().build()
        val cases = listOf(
            listOf(titleRow(), emptyList()) to "NOT_FOUND",
            listOf(titleRow(), listOf(mapOf("status" to "ARCHIVED"))) to "PERSON_ARCHIVED",
            listOf(titleRow(), listOf(mapOf("status" to "ACTIVE")), emptyList()) to "FIELD_INVALID",
            listOf(titleRow(), listOf(mapOf("status" to "ACTIVE")), role(), listOf(mapOf("exists" to 1))) to "CREDIT_DUPLICATE",
        )
        for ((rows, code) in cases) {
            val db = database("WriteCredit", CatalogWriteIntent.credit(request), *rows.toTypedArray())
            val result = RpcResult<Spoolin.Outcome>()
            CreditWrites(owner(db), clock).writeCredit(request, result)
            assertEquals(code, if (result.failure != null) Status.fromThrowable(result.failure).description else result.value().problem.code)
            assertEquals(1, db.writes.size)
        }
    }

    @Test fun `credit updates and removals reject absent or stale versions`() = withMaintainer {
        for ((rows, code) in listOf(emptyList<Map<String, Any?>>() to "NOT_FOUND", creditRow(6) to "VERSION_CONFLICT")) {
            val request = creditRequest().setId(id.toString()).build()
            val db = database("WriteCredit", CatalogWriteIntent.credit(request), rows)
            val result = RpcResult<Spoolin.Outcome>()
            CreditWrites(owner(db), clock).writeCredit(request, result)
            assertEquals(code, Status.fromThrowable(result.failure).description)
            val remove = Spoolin.VersionedId.newBuilder().setContext(context).setId(id.toString()).setExpectedVersion(7).build()
            val removalDb = database("RemoveCredit", CatalogWriteIntent.remove(remove), rows)
            val removal = RpcResult<Spoolin.Outcome>()
            CreditWrites(owner(removalDb), clock).removeCredit(remove, removal)
            assertEquals(code, Status.fromThrowable(removal.failure).description)
        }
    }

}
