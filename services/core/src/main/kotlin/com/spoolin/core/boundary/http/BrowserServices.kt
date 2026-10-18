package com.spoolin.core.boundary.http

import com.spoolin.support.observability.LogicalMetrics
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

/** Providers retain per-request availability checks for identity, admission and observations. */
@Component
class BrowserServices(
    val identity: ObjectProvider<BrowserIdentity>,
    val admission: ObjectProvider<BrowserAdmission>,
    val metrics: ObjectProvider<LogicalMetrics>,
)
