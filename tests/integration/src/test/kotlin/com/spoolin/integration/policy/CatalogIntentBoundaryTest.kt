package com.spoolin.integration.policy

import com.google.protobuf.FieldMask
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Actual signed mTLS owner writes, live Core session checks and independently read PostgreSQL effects. */
class CatalogIntentBoundaryTest {
    @Test
    fun `changed person biography presence and version cannot replay a committed different request`() {
        CatalogPolicyFixture().use { fixture ->
            val original =
                fixture
                    .person("Person")
                    .toBuilder()
                    .setValues(
                        Spoolin.PersonValues
                            .newBuilder()
                            .setName("Person")
                            .setBiography("Original"),
                    ).build()
            val result = fixture.person(original)
            val variants =
                listOf(
                    original.toBuilder().setValues(original.values.toBuilder().setBiography("Changed")).build(),
                    original.toBuilder().setValues(original.values.toBuilder().clearBiography()).build(),
                    original.toBuilder().setExpectedVersion(0).build(),
                    original.toBuilder().setUpdateMask(FieldMask.newBuilder().addPaths("values.biography")).build(),
                )
            variants.forEach { conflict { fixture.person(it) } }
            assertEquals(result, fixture.person(original))
            assertEquals("Original", fixture.text("SELECT biography FROM person WHERE id=?", result.resultId))
            assertEquals(1, fixture.count("SELECT count(*) FROM person"))
            assertEquals(1, fixture.count("SELECT count(*) FROM catalog_audit_event"))
        }
    }

    @Test
    fun `movie original text language date runtime and countries are bound before replay`() {
        CatalogPolicyFixture().use { fixture ->
            val original = fixture.movie("Movie")
            val values = original.values
            val result = fixture.movie(original)
            val variants =
                listOf(
                    original.toBuilder().setValues(values.toBuilder().setOriginalTitle("Original")).build(),
                    original.toBuilder().setValues(values.toBuilder().setOriginalLanguage("fr")).build(),
                    original.toBuilder().setValues(values.toBuilder().setSynopsisLanguage("fr")).build(),
                    original.toBuilder().setValues(values.toBuilder().setRuntimeMinutes(90)).build(),
                    original.toBuilder().setValues(values.toBuilder().addCountries("GB")).build(),
                    original.toBuilder()
                        .setValues(values.toBuilder().setRelease(values.release.toBuilder().setMonth(2))).build(),
                    original.toBuilder()
                        .setValues(values.toBuilder().setRelease(values.release.toBuilder().setDay(1))).build(),
                    original.toBuilder().setExpectedVersion(0).build(),
                )
            variants.forEach { conflict { fixture.movie(it) } }
            assertEquals(result, fixture.movie(original))
            assertEquals(1, fixture.count("SELECT count(*) FROM title"))
            assertEquals(1, fixture.count("SELECT count(*) FROM catalog_audit_event"))
        }
    }

    @Test
    fun `credit ordered characters masks versions and removal versions have complete receipt hashes`() {
        CatalogPolicyFixture().use { fixture ->
            val title = fixture.movie(fixture.movie("Movie"))
            val person = fixture.person(fixture.person("Person"))
            val role = fixture.characterRole()
            val original =
                fixture
                    .credit(title.resultId, person.resultId, role, "First")
                    .toBuilder()
                    .addCharacters("Second")
                    .build()
            val result = fixture.credit(original)
            conflict {
                fixture.credit(
                    original
                        .toBuilder()
                        .clearCharacters()
                        .addCharacters("Second")
                        .addCharacters("First")
                        .build(),
                )
            }
            conflict { fixture.credit(original.toBuilder().setExpectedVersion(1).build()) }
            conflict {
                fixture.credit(
                    original.toBuilder().setUpdateMask(FieldMask.newBuilder().addPaths("characters")).build(),
                )
            }
            assertEquals(result, fixture.credit(original))
            val remove =
                Spoolin.VersionedId
                    .newBuilder()
                    .setContext(fixture.context())
                    .setId(result.resultId)
                    .setExpectedVersion(0)
                    .build()
            val removed = fixture.remove(remove)
            assertEquals("SUCCEEDED", removed.state)
            conflict { fixture.remove(remove.toBuilder().setExpectedVersion(1).build()) }
            assertEquals(removed, fixture.remove(remove))
            assertEquals(0, fixture.count("SELECT count(*) FROM credit"))
        }
    }

    @Test
    fun `two concurrent same-UUID writes commit once and lost reply recovers the retained owner outcome`() {
        CatalogPolicyFixture().use { fixture ->
            val request = fixture.person("Concurrent")
            val executor = Executors.newFixedThreadPool(2)
            val start = CountDownLatch(1)
            try {
                val responses =
                    List(2) {
                        executor.submit<Spoolin.Outcome> {
                            start.await()
                            fixture.person(request)
                        }
                    }
                start.countDown()
                val results = responses.map { it.get(10, TimeUnit.SECONDS) }
                assertEquals(results[0], results[1])
                assertEquals(1, fixture.count("SELECT count(*) FROM person"))
                assertEquals(1, fixture.count("SELECT count(*) FROM catalog_audit_event"))
            } finally {
                executor.shutdownNow()
            }
            val lost = fixture.person("Lost reply")
            fixture.loseNextPersonReply.set(true)
            val failure = assertThrows(StatusRuntimeException::class.java) { fixture.person(lost) }
            assertEquals(Status.Code.UNAVAILABLE, failure.status.code)
            val retained = fixture.lookup(lost.context.operationId)
            assertEquals("SUCCEEDED", retained.state)
            assertEquals(retained, fixture.person(lost))
            assertEquals(2, fixture.count("SELECT count(*) FROM person"))
            assertEquals(2, fixture.count("SELECT count(*) FROM catalog_audit_event"))
        }
    }

    @Test
    fun `legacy incomplete fingerprint fails closed while original lookup and historical hash remain intact`() {
        CatalogPolicyFixture().use { fixture ->
            val legacy =
                fixture
                    .person("Legacy")
                    .toBuilder()
                    .setValues(
                        Spoolin.PersonValues
                            .newBuilder()
                            .setName("Legacy")
                            .setBiography("Original"),
                    ).build()
            val person = fixture.legacyPerson(legacy)
            val before = fixture.receipt(legacy.context.operationId)
            val retained = fixture.lookup(legacy.context.operationId)
            assertEquals("SUCCEEDED", retained.state)
            assertEquals(person.toString(), retained.resultId)
            conflict { fixture.person(legacy) }
            conflict {
                fixture.person(legacy.toBuilder().setValues(legacy.values.toBuilder().setBiography("Changed")).build())
            }
            assertEquals(before, fixture.receipt(legacy.context.operationId))
            assertEquals(retained, fixture.lookup(legacy.context.operationId))
            assertEquals("Original", fixture.text("SELECT biography FROM person WHERE id=?", person.toString()))
            assertEquals(1, fixture.count("SELECT count(*) FROM person"))
            assertEquals(1, fixture.count("SELECT count(*) FROM catalog_audit_event"))
        }
    }

    @Test
    fun `role raw input and category are bound while update requires present expected version`() {
        CatalogPolicyFixture().use { fixture ->
            val role = fixture.roleRequest("Role")
            val result = fixture.role(role)
            conflict { fixture.role(role.toBuilder().setCategory("CREATOR").build()) }
            conflict { fixture.role(role.toBuilder().setLabel(" Role ").build()) }
            assertEquals(result, fixture.role(role))
            val person = fixture.person(fixture.person("Person"))
            val missingVersion =
                fixture
                    .person("Changed")
                    .toBuilder()
                    .setId(person.resultId)
                    .setUpdateMask(FieldMask.newBuilder().addPaths("values.name"))
                    .build()
            assertEquals("FIELD_INVALID", fixture.person(missingVersion).problem.code)
            assertEquals("Person", fixture.text("SELECT name FROM person WHERE id=?", person.resultId))
        }
    }

    private fun conflict(action: () -> Unit) {
        val failure = assertThrows(StatusRuntimeException::class.java, action)
        assertEquals(Status.Code.ALREADY_EXISTS, failure.status.code)
        assertEquals("IDEMPOTENCY_CONFLICT", failure.status.description)
    }
}
