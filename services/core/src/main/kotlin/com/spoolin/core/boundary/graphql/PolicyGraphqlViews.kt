package com.spoolin.core.boundary.graphql

import com.spoolin.core.features.artworkproxy.ArtworkGraphql
import com.spoolin.core.features.catalogview.credit.ManagedCreditGraphql
import com.spoolin.core.features.catalogview.lifecycle.LifecycleGraphql
import com.spoolin.core.features.catalogview.movie.ManagedCatalogGraphql
import com.spoolin.core.features.catalogview.person.ManagedPersonGraphql
import com.spoolin.core.features.catalogview.read.CatalogViewGraphql
import com.spoolin.core.features.draft.DraftGraphql
import org.springframework.beans.factory.ObjectProvider

/** Optional GraphQL feature modules bound into [PolicyGraphql]. */
data class PolicyGraphqlViews(
    val accounts: AccountGraphql? = null,
    val catalog: CatalogViewGraphql? = null,
    val drafts: DraftGraphql? = null,
    val managedCatalog: ManagedCatalogGraphql? = null,
    val managedPerson: ManagedPersonGraphql? = null,
    val managedCredit: ManagedCreditGraphql? = null,
    val artwork: ArtworkGraphql? = null,
    val lifecycle: LifecycleGraphql? = null,
)

/** Spring-resolved feature modules; protected draft recovery is required for Catalog writes. */
data class PolicyGraphqlProviders(
    val accounts: ObjectProvider<AccountGraphql>,
    val catalog: ObjectProvider<CatalogViewGraphql>,
    val drafts: DraftGraphql,
    val managedCatalog: ObjectProvider<ManagedCatalogGraphql>,
    val managedPerson: ObjectProvider<ManagedPersonGraphql>,
    val managedCredit: ObjectProvider<ManagedCreditGraphql>,
    val artwork: ObjectProvider<ArtworkGraphql>,
    val lifecycle: ObjectProvider<LifecycleGraphql>,
) {
    fun resolved(): PolicyGraphqlViews =
        PolicyGraphqlViews(
            accounts = accounts.ifAvailable,
            catalog = catalog.ifAvailable,
            drafts = drafts,
            managedCatalog = managedCatalog.ifAvailable,
            managedPerson = managedPerson.ifAvailable,
            managedCredit = managedCredit.ifAvailable,
            artwork = artwork.ifAvailable,
            lifecycle = lifecycle.ifAvailable,
        )
}
