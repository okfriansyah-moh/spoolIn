package com.spoolin.catalog.features.artwork

import com.spoolin.support.notifications.ExhaustionNotifications
import javax.sql.DataSource

/** Retained wrapper keeps the artwork dispatcher on the shared owner-local exhaustion path. */
class ArtworkNotifications(private val recorder: ExhaustionNotifications) {
    constructor(dataSource: DataSource, mode: String) : this(ExhaustionNotifications(dataSource, "catalog", mode))

    fun recordExhaustion(): Int = recorder.recordExhaustion()
}
