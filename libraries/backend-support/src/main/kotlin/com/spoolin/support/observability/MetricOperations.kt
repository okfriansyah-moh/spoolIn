package com.spoolin.support.observability

internal data class MetricOperation(
    val name: String,
    val boundary: String,
    val response: String,
    val mutation: Boolean,
    val receiptLookup: Boolean = false,
)

/** Source-owned operation names only; caller supplied operation names and identifiers are never labels. */
internal object MetricOperations {
    private val catalogReads = mapOf(
        "SearchTitles" to "TitlePage", "GetTitle" to "Title", "SearchPeople" to "PersonPage",
        "GetPerson" to "Person", "PersonCredits" to "CreditPage", "TitleCredits" to "CreditPage",
        "ListGenres" to "Genres", "ListRoles" to "Roles", "ManagedTitles" to "TitlePage",
        "ManagedPeople" to "PersonPage", "ManagedTitle" to "Title", "ManagedPerson" to "Person",
        "ManagedCredits" to "CreditPage", "Audit" to "AuditPage", "DeletionImpact" to "Impact",
        "GetOperation" to "Outcome", "GetAttachment" to "Attachment", "ResolveArtworkAccess" to "ArtworkGrant",
    )
    private val catalogWrites = mapOf(
        "CreateRole" to "Outcome", "WriteMovie" to "Outcome", "WritePerson" to "Outcome",
        "WriteCredit" to "Outcome", "RemoveCredit" to "Outcome", "MovieLifecycle" to "Outcome",
        "PersonLifecycle" to "Outcome", "BeginArtwork" to "Outcome", "CancelArtwork" to "ArtworkCancelOutcome",
        "ProposeMovie" to "ProposalRevision", "ApproveAndCreate" to "Outcome", "RejectProposal" to "Outcome",
    )
    private val mediaReads = mapOf("GetAsset" to "Asset", "GetOperation" to "Outcome", "ReadAsset" to "AssetReadChunk")
    private val mediaWrites = mapOf("Upload" to "Outcome", "Reserve" to "Attachment", "Confirm" to "Attachment", "Release" to "Attachment")
    private val gatewayReads = mapOf("GetPolicy" to "PolicySnapshot", "GetMovieContext" to "MovieContext", "GetMovieOperation" to "Outcome")
    private val gatewayWrites = mapOf("ProposeMovie" to "ProposalRevision", "ApproveMovie" to "Outcome", "RejectMovie" to "Outcome")
    private val rpc = mapOf(
        "catalog" to methods("CatalogService", "owner", catalogReads, catalogWrites),
        "media" to methods("MediaService", "owner", mediaReads, mediaWrites),
        "core" to methods("McpGatewayService", "ingress", gatewayReads, gatewayWrites),
    )
    private val graphqlReads = setOf("session", "policy", "titles", "title", "people", "person", "titleCredits",
        "personCredits", "genres", "creditRoles", "managedTitle", "managedPerson", "managedCredits",
        "managedTitles", "managedPeople", "deletionImpact", "operation", "drafts", "audit")
    private val graphqlWrites = setOf("register", "signIn", "signOut", "createMovie", "updateMovie", "movieLifecycle",
        "createPerson", "updatePerson", "personLifecycle", "createRole", "addCredit", "updateCredit", "removeCredit",
        "uploadArtwork", "cancelArtwork", "saveDraft", "discardDraft")

    fun rpc(service: String, method: String): MetricOperation? = rpc[service]?.get(method)

    fun named(service: String, name: String, boundary: String): MetricOperation? {
        val graphql = if (service == "core") graphql(name) else null
        return (graphql ?: rpc[service]?.values?.find { it.name == name })?.takeIf { it.boundary == boundary }
    }

    fun graphql(field: String): MetricOperation? = when (field) {
        in graphqlReads -> MetricOperation(field, "ingress", "", false, field == "operation")
        in graphqlWrites -> MetricOperation(field, "ingress", "", true)
        else -> null
    }

    private fun methods(service: String, boundary: String, reads: Map<String, String>, writes: Map<String, String>) =
        (reads + writes).map { (name, response) ->
            "spoolin.v1.$service/$name" to MetricOperation(name, boundary, "spoolin.v1.$response",
                name in writes, name in setOf("GetOperation", "GetMovieOperation"))
        }.toMap()
}
