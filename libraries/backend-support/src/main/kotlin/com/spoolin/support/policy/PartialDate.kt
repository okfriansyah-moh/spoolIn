package com.spoolin.support.policy

import java.time.DateTimeException
import java.time.LocalDate
import java.time.YearMonth

private const val LAST_MONTH = 12
private const val MAXIMUM_YEAR = 9999

class PartialDate private constructor(
    val year: Int?,
    val month: Int?,
    val day: Int?,
) {
    val precision: String
        get() =
            when {
                day != null -> "DAY"
                month != null -> "MONTH"
                year != null -> "YEAR"
                else -> "UNKNOWN"
            }

    fun earliest(): LocalDate? = year?.let { LocalDate.of(it, month ?: 1, day ?: 1) }

    fun latest(): LocalDate? =
        year?.let {
            LocalDate.of(
                it,
                month ?: LAST_MONTH,
                day ?: YearMonth.of(it, month ?: LAST_MONTH).lengthOfMonth(),
            )
        }

    companion object {
        fun of(
            year: Int? = null,
            month: Int? = null,
            day: Int? = null,
            requireYear: Boolean = false,
        ): PartialDate {
            validatePresence(year, month, day, requireYear)
            validateCalendar(year, month, day)
            return PartialDate(year, month, day)
        }

        fun ordered(
            start: PartialDate,
            end: PartialDate,
        ): Boolean? {
            val first = start.earliest()
            val last = end.latest()
            return when {
                first == null || last == null -> null
                last < first -> false
                requireNotNull(end.earliest()) >= requireNotNull(start.latest()) -> true
                else -> null
            }
        }

        private fun validatePresence(
            year: Int?,
            month: Int?,
            day: Int?,
            requireYear: Boolean,
        ) {
            val missingYear = year == null && (requireYear || month != null)
            val missingMonth = day != null && month == null
            if (missingYear || missingMonth) throw InputViolation("release", "Supply the enclosing year and month.")
        }

        private fun validateCalendar(
            year: Int?,
            month: Int?,
            day: Int?,
        ) {
            if (year != null && year !in 1..MAXIMUM_YEAR) {
                throw InputViolation("release.year", "Use a year from 1 to 9999.")
            }
            try {
                if (month != null) LocalDate.of(requireNotNull(year), month, day ?: 1)
            } catch (_: DateTimeException) {
                throw InputViolation("release", "Use a valid calendar date.")
            }
        }
    }
}
