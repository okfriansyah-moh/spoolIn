package com.spoolin.catalog.boundary.policy

import spoolin.v1.Spoolin
import java.nio.ByteBuffer
import java.util.Base64

/** Versioned typed fingerprint input. Values exist only in memory before the existing owner HMAC. */
object CatalogWriteIntent {
    fun movie(request: Spoolin.MovieWrite): List<Pair<String, String?>> {
        val values = request.values
        return marker() + listOf(
            "id" to raw(request.id.takeIf { request.hasId() }),
            "expectedVersion" to request.expectedVersion.takeIf { request.hasExpectedVersion() }?.toString(),
            "values.present" to request.hasValues().toString(),
            "title" to raw(values.title),
            "originalTitle" to raw(values.originalTitle.takeIf { values.hasOriginalTitle() }),
            "synopsis" to raw(values.synopsis),
            "synopsisLanguage" to raw(values.synopsisLanguage.takeIf { values.hasSynopsisLanguage() }),
            "originalLanguage" to raw(values.originalLanguage.takeIf { values.hasOriginalLanguage() }),
            "runtimeMinutes" to values.runtimeMinutes.takeIf { values.hasRuntimeMinutes() }?.toString(),
            "release.present" to values.hasRelease().toString(),
            "release.year" to values.release.year.takeIf { values.release.hasYear() }?.toString(),
            "release.month" to values.release.month.takeIf { values.release.hasMonth() }?.toString(),
            "release.day" to values.release.day.takeIf { values.release.hasDay() }?.toString(),
            "mask.present" to request.hasUpdateMask().toString(),
        ) + repeated("genres", values.genreIdsList.distinct().sorted()) + repeated("countries", values.countriesList) +
            repeated("mask", request.updateMask.pathsList.distinct().sorted())
    }

    fun person(request: Spoolin.PersonWrite): List<Pair<String, String?>> = marker() + listOf(
        "id" to raw(request.id.takeIf { request.hasId() }),
        "expectedVersion" to request.expectedVersion.takeIf { request.hasExpectedVersion() }?.toString(),
        "values.present" to request.hasValues().toString(),
        "name" to raw(request.values.name),
        "biography" to raw(request.values.biography.takeIf { request.values.hasBiography() }),
        "mask.present" to request.hasUpdateMask().toString(),
    ) + repeated("mask", request.updateMask.pathsList.distinct().sorted())

    fun credit(request: Spoolin.CreditWrite): List<Pair<String, String?>> = marker() + listOf(
        "id" to raw(request.id.takeIf { request.hasId() }),
        "expectedVersion" to request.expectedVersion.takeIf { request.hasExpectedVersion() }?.toString(),
        "titleId" to raw(request.titleId), "personId" to raw(request.personId), "roleId" to raw(request.roleId),
        "mask.present" to request.hasUpdateMask().toString(),
    ) + repeated("characters", request.charactersList) + repeated("mask", request.updateMask.pathsList.distinct().sorted())

    fun role(request: Spoolin.RoleWrite): List<Pair<String, String?>> = marker() + listOf(
        "category" to raw(request.category), "label" to raw(request.label),
    )

    fun remove(request: Spoolin.VersionedId): List<Pair<String, String?>> = marker() + listOf(
        "id" to raw(request.id), "expectedVersion" to request.expectedVersion.takeIf { request.hasExpectedVersion() }?.toString(),
    )

    private fun marker(): List<Pair<String, String?>> = listOf("intentSchema" to "catalog-write-v2")

    private fun repeated(name: String, values: List<String>): List<Pair<String, String?>> =
        listOf("$name.count" to values.size.toString()) + values.mapIndexed { index, value -> "$name[$index]" to raw(value) }

    private fun raw(value: String?): String? {
        if (value == null) return null
        // Java strings may contain a rejected surrogate before validation. Preserve every code unit in the hash.
        val bytes = ByteBuffer.allocate(value.length * Char.SIZE_BYTES)
        value.forEach { bytes.putChar(it) }
        return Base64.getEncoder().encodeToString(bytes.array())
    }
}
