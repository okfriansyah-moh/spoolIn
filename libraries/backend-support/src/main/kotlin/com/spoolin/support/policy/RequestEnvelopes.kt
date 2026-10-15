package com.spoolin.support.policy

private const val CAPACITY_UUID = "00000000-0000-0000-0000-000000000000"
private const val POLICY_REVISION_LENGTH = 64
private const val CAPACITY_YEAR = 9999

internal data class CapacityText(
    val field: String,
)

/** Generated-SDL operations and the movie_proposal DTO include required sibling fields. */
internal object RequestEnvelopes {
    fun forField(field: String): Map<String, Any> =
        when (field) {
            "registrationName", "email", "password" -> registration()
            "personName", "biography" -> person(field)
            "title", "originalTitle", "synopsis" -> movie(field)
            "creditRole" -> role()
            "characterName" -> credit()
            "search" ->
                mapOf(
                    "query" to "query(\$filter:SearchInput!){titles(filter:\$filter){nodes{id}}}",
                    "variables" to mapOf("filter" to mapOf("text" to CapacityText(field))),
                )
            else -> proposal()
        }

    private fun mutation(
        type: String,
        operation: String,
        input: Map<String, Any>,
    ): Map<String, Any> =
        mapOf(
            "query" to "mutation(\$id:UUID!,\$p:String!,\$input:$type!){" +
                "$operation(operationId:\$id,policyRevision:\$p,input:\$input){problem{code}}}",
            "variables" to mapOf("id" to CAPACITY_UUID, "p" to "0".repeat(POLICY_REVISION_LENGTH), "input" to input),
        )

    private fun movie(field: String): Map<String, Any> {
        val input =
            movieFields() + if (field == "originalTitle") mapOf("originalTitle" to CapacityText(field)) else emptyMap()
        return mutation("MovieInput", "createMovie", input)
    }

    private fun movieFields(): Map<String, Any> =
        mapOf(
            "title" to CapacityText("title"),
            "synopsis" to CapacityText("synopsis"),
            "genreIds" to listOf(CAPACITY_UUID),
            "release" to mapOf("year" to CAPACITY_YEAR),
        )

    private fun person(field: String): Map<String, Any> {
        val input =
            mapOf("name" to CapacityText("personName")) +
                if (field == "biography") mapOf("biography" to CapacityText(field)) else emptyMap()
        return mutation("PersonInput", "createPerson", input)
    }

    private fun registration(): Map<String, Any> =
        mapOf(
            "query" to
                "mutation(\$id:UUID!,\$input:RegisterInput!){register(operationId:\$id,input:\$input){problem{code}}}",
            "variables" to
                mapOf(
                    "id" to CAPACITY_UUID,
                    "input" to
                        mapOf(
                            "name" to CapacityText("registrationName"),
                            "email" to CapacityText("email"),
                            "password" to CapacityText("password"),
                        ),
                ),
        )

    private fun role(): Map<String, Any> =
        mapOf(
            "query" to "mutation(\$id:UUID!,\$p:String!,\$label:String!){" +
                "createRole(operationId:\$id,policyRevision:\$p,category:CAST,label:\$label){problem{code}}}",
            "variables" to
                mapOf(
                    "id" to CAPACITY_UUID,
                    "p" to "0".repeat(POLICY_REVISION_LENGTH),
                    "label" to CapacityText("creditRole"),
                ),
        )

    private fun credit(): Map<String, Any> =
        mapOf(
            "query" to "mutation(\$id:UUID!,\$p:String!,\$v:Long!,\$input:CreditInput!){" +
                "addCredit(operationId:\$id,titleId:\$id,expectedVersion:\$v," +
                "policyRevision:\$p,input:\$input){problem{code}}}",
            "variables" to
                mapOf(
                    "id" to CAPACITY_UUID,
                    "p" to "0".repeat(POLICY_REVISION_LENGTH),
                    "v" to "1",
                    "input" to
                        mapOf(
                            "personId" to CAPACITY_UUID,
                            "roleId" to CAPACITY_UUID,
                            "characters" to listOf(CapacityText("characterName")),
                        ),
                ),
        )

    private fun proposal(): Map<String, Any> =
        movieFields() +
            mapOf(
                "operationId" to CAPACITY_UUID,
                "sources" to
                    listOf(
                        mapOf(
                            "field" to "title",
                            "url" to CapacityText("sourceUrl"),
                            "reuseBasis" to CapacityText("reuseBasis"),
                        ),
                    ),
            )
}
