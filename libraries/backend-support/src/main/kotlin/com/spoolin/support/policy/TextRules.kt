package com.spoolin.support.policy

private const val PASSWORD_UTF8_LIMIT = 512
private const val SURROGATE_START = 0xD800
private const val SURROGATE_END = 0xDFFF
private const val MAXIMUM_GENRES = 20

data class TextBound(
    val minCodePoints: Int,
    val maxCodePoints: Int,
    val required: Boolean = true,
    val normalization: String = "TRIM_WHITE_SPACE",
) {
    init {
        require(minCodePoints >= 0 && maxCodePoints >= minCodePoints)
        require(!required || minCodePoints >= 1)
        require(normalization in setOf("TRIM_WHITE_SPACE", "EXACT"))
    }
}

class InputViolation(
    val path: String,
    val correction: String,
) : IllegalArgumentException("$path: $correction")

sealed interface PatchValue<out T> {
    data object Omitted : PatchValue<Nothing>

    data object Clear : PatchValue<Nothing>

    data class Value<T>(
        val value: T,
    ) : PatchValue<T>
}

object TextRules {
    fun blank(value: String): Boolean = value.all(::whiteSpace)

    fun ordinary(
        value: String,
        bound: TextBound,
        path: String = "value",
    ): String {
        scalars(value, path)
        val trimmed = if (bound.normalization == "EXACT") value else value.trim(::whiteSpace)
        if (bound.normalization == "EXACT" && trimmed.toByteArray(Charsets.UTF_8).size > PASSWORD_UTF8_LIMIT) {
            throw InputViolation(path, "Use at most 512 UTF-8 bytes.")
        }
        val count = trimmed.codePointCount(0, trimmed.length)
        if (count !in bound.minCodePoints..bound.maxCodePoints) {
            throw InputViolation(path, "Use ${bound.minCodePoints} to ${bound.maxCodePoints} Unicode code points.")
        }
        return trimmed
    }

    fun scalars(
        value: String,
        path: String,
    ) {
        if (value.codePoints().anyMatch { it == 0 || it in SURROGATE_START..SURROGATE_END }) {
            throw InputViolation(path, "Remove invalid Unicode characters.")
        }
    }

    fun optional(
        value: String?,
        present: Boolean,
        bound: TextBound,
        path: String,
    ): PatchValue<String> =
        when {
            !present -> PatchValue.Omitted
            value == null -> PatchValue.Clear
            else -> optionalText(value, bound, path)
        }

    fun changed(
        stored: String?,
        value: String,
        bound: TextBound,
        path: String,
    ): String = if (stored == value) stored else ordinary(value, bound, path)

    fun characters(
        values: List<String>,
        bound: TextBound,
    ): List<String> {
        val result =
            values.mapIndexedNotNull { index, value ->
                if (blank(value)) null else ordinary(value, bound, "characters[$index]")
            }
        if (result.distinct().size !=
            result.size
        ) {
            throw InputViolation("characters", "Remove duplicate character names.")
        }
        return result
    }

    fun genres(values: List<String>): Set<String> {
        if (values.isEmpty()) throw InputViolation("genreIds", "Choose at least one genre.")
        if (values.size > MAXIMUM_GENRES) throw InputViolation("genreIds", "Choose at most 20 genres.")
        return values.toSet()
    }

    private fun optionalText(
        value: String,
        bound: TextBound,
        path: String,
    ): PatchValue<String> {
        scalars(value, path)
        return if (value
                .trim(
                    ::whiteSpace,
                ).isEmpty()
        ) {
            PatchValue.Clear
        } else {
            PatchValue.Value(ordinary(value, bound, path))
        }
    }

    private fun whiteSpace(char: Char): Boolean =
        char in '\u0009'..'\u000d' || char in '\u2000'..'\u200a' ||
            char in "\u0020\u0085\u00a0\u1680\u2028\u2029\u202f\u205f\u3000"
}
