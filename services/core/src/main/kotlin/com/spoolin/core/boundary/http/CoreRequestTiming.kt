package com.spoolin.core.boundary.http

import com.spoolin.support.observability.LogicalResult
import jdk.jfr.Category
import jdk.jfr.Event
import jdk.jfr.Label
import jdk.jfr.Name
import jdk.jfr.StackTrace

/** Optional operator recording of bounded metadata outcomes; request values and identities are never fields. */
@Name("spoolin.CoreRequestTiming")
@Label("Core metadata request completion")
@Category("SpoolIn")
@StackTrace(false)
internal class CoreRequestTiming : Event() {
    @JvmField var operation: String = ""

    @JvmField var outcome: String = ""

    @JvmField var code: String = ""

    @JvmField var durationNanos: Long = 0

    fun complete(
        root: String,
        result: LogicalResult,
        startedNanos: Long,
    ) {
        end()
        if (root !in OPERATIONS) return
        operation = root
        outcome = result.outcome.name
        code = result.code.name
        durationNanos = (System.nanoTime() - startedNanos).coerceAtLeast(0)
        commit()
    }

    companion object {
        private val OPERATIONS = setOf("titles", "title", "updateMovie", "updatePerson", "updateCredit")

        fun beginIfEnabled(): CoreRequestTiming? = CoreRequestTiming().takeIf { it.isEnabled }?.also { it.begin() }
    }
}
