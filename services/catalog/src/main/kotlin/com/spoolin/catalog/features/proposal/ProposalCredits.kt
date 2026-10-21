package com.spoolin.catalog.features.proposal

import com.spoolin.catalog.boundary.policy.CatalogWritePolicy
import com.spoolin.catalog.features.credit.CreditValidation
import com.spoolin.catalog.operations.audit.AuditInsert
import com.spoolin.support.policy.TextRules
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import spoolin.v1.Spoolin
import java.sql.Connection
import java.time.Instant
import java.util.UUID

internal object ProposalCredits {
    fun normalize(credits: List<Spoolin.ProposalCredit>): List<Spoolin.ProposalCredit> {
        require(credits.size <= 100) { "Too many credits" }
        val policy = CatalogWritePolicy.environment()
        val result = credits.map { credit ->
            require(credit.hasPersonId() != credit.hasPersonName()) { "Supply personId or personName" }
            uuid(credit.roleId)
            val builder = credit.toBuilder().clearPersonLabel().clearRoleLabel()
            if (credit.hasPersonId()) uuid(credit.personId)
            else builder.personName = TextRules.ordinary(credit.personName, policy.textBounds.getValue("personName"), "personName")
            builder.clearCharacterNames().addAllCharacterNames(TextRules.characters(credit.characterNamesList, policy.textBounds.getValue("characterName")))
            require(!credit.hasBillingOrder() || credit.billingOrder >= 0) { "Billing order must be nonnegative" }
            builder.build()
        }
        require(result.map { (if (it.hasPersonId()) "id:${it.personId}" else "name:${it.personName}") to it.roleId }.distinct().size == result.size) { "Duplicate credit" }
        return result
    }

    fun validate(conn: Connection, credits: List<Spoolin.ProposalCredit>) {
        credits.forEach { credit ->
            if (credit.hasPersonId()) {
                require(conn.queryBound("SELECT status FROM person WHERE id=? FOR SHARE", listOf(uuid(credit.personId))) { it.getString(1) }.singleOrNull() == "ACTIVE") { "Person must be active" }
            }
            val role = conn.queryBound("SELECT category,allows_characters FROM credit_role WHERE id=? FOR SHARE", listOf(uuid(credit.roleId))) { it.getString(1) to it.getBoolean(2) }.singleOrNull()
                ?: throw IllegalArgumentException("Choose a known role")
            CreditValidation.validateCharactersForRole(role.second, role.first, credit.characterNamesList)
        }
    }

    fun publish(conn: Connection, credits: List<Spoolin.ProposalCredit>, title: UUID, actor: UUID, operation: UUID, correlation: UUID) {
        validate(conn, credits)
        val seen = mutableSetOf<Pair<UUID, UUID>>()
        credits.forEachIndexed { index, credit ->
            val person = if (credit.hasPersonId()) uuid(credit.personId) else namedPerson(conn, credit.personName, actor, operation, correlation)
            val role = uuid(credit.roleId)
            require(seen.add(person to role)) { "Duplicate credit resolves to same person and role" }
            val id = UUID.nameUUIDFromBytes("proposal-credit:$operation:$index".toByteArray(Charsets.UTF_8))
            conn.executeBound("INSERT INTO credit(id,title_id,person_id,role_id,characters,billing_order) VALUES (?,?,?,?,?::text[],?)",
                id, title, person, role, conn.createArrayOf("text", credit.characterNamesList.toTypedArray()), credit.billingOrder.takeIf { credit.hasBillingOrder() })
            conn.executeBound("UPDATE title SET version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?", title)
            conn.executeBound("UPDATE person SET impact_version=impact_version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?", person)
            AuditInsert.record(conn, operation, actor, "HUMAN", "ADD_CREDIT", "CREDIT", id, "SUCCEEDED", "{}", correlation, Instant.now())
        }
    }

    private fun namedPerson(conn: Connection, name: String, actor: UUID, operation: UUID, correlation: UUID): UUID {
        conn.queryBound("SELECT pg_advisory_xact_lock(hashtextextended(?, 41002))", listOf(name)) { 1 }
        val matches = conn.queryBound("SELECT id FROM person WHERE name=? AND status='ACTIVE' ORDER BY id FOR UPDATE", listOf(name)) { it.getObject(1, UUID::class.java) }
        require(matches.size <= 1) { "Multiple people share this name; supply personId" }
        matches.singleOrNull()?.let { return it }
        val id = UUID.randomUUID()
        conn.executeBound("INSERT INTO person(id,name) VALUES (?,?)", id, name)
        AuditInsert.record(conn, operation, actor, "HUMAN", "CREATE_PERSON", "PERSON", id, "SUCCEEDED", "{}", correlation, Instant.now())
        return id
    }

    fun content(credit: Spoolin.ProposalCredit): Map<String, Any?> = mapOf(
        "person_id" to credit.personId.takeIf { credit.hasPersonId() }, "person_name" to credit.personName.takeIf { credit.hasPersonName() },
        "role_id" to credit.roleId, "character_names" to credit.characterNamesList,
        "billing_order" to credit.billingOrder.takeIf { credit.hasBillingOrder() },
    )

    fun fromContent(content: Map<String, Any?>): Spoolin.ProposalCredit = Spoolin.ProposalCredit.newBuilder().apply {
        (content["person_id"] as? String)?.let(::setPersonId)
        (content["person_name"] as? String)?.let(::setPersonName)
        roleId = content["role_id"] as String
        @Suppress("UNCHECKED_CAST")
        addAllCharacterNames(content["character_names"] as? List<String> ?: emptyList())
        (content["billing_order"] as? Number)?.toInt()?.let(::setBillingOrder)
    }.build()

    fun labels(conn: Connection, credit: Spoolin.ProposalCredit): Spoolin.ProposalCredit {
        val person = if (credit.hasPersonName()) credit.personName else conn.queryBound("SELECT name FROM person WHERE id=?", listOf(uuid(credit.personId))) { it.getString(1) }.singleOrNull()
        val role = conn.queryBound("SELECT label FROM credit_role WHERE id=?", listOf(uuid(credit.roleId))) { it.getString(1) }.singleOrNull()
        return credit.toBuilder().apply { person?.let(::setPersonLabel); role?.let(::setRoleLabel) }.build()
    }

    private fun uuid(value: String): UUID = UUID.fromString(value).also { require(it.toString() == value) }
}
