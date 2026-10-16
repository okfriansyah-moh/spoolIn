package com.spoolin.support.policy

class ProtectedReference(
    private val reference: String,
    private val mode: String,
    private val symlink: Boolean,
    private val bytes: Int,
    private val purpose: String,
) {
    fun validate(
        setting: String,
        requiredPurpose: String,
        expectedBytes: Int = 32,
    ) {
        val protected = !TextRules.blank(reference) && mode == "0600" && !symlink
        val compatible = bytes == expectedBytes && purpose == requiredPurpose
        if (!protected || !compatible) {
            throw ConfigurationViolation(
                setting,
                "Supply an owner-only protected file for the selected purpose and size.",
            )
        }
    }

    override fun toString(): String = "ProtectedReference(redacted)"
}
