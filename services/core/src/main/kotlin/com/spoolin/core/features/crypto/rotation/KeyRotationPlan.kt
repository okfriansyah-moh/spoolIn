package com.spoolin.core.features.crypto.rotation

/** Coordinated alias rotation plan (T040 source; runtime rotation pending). */
data class KeyRotationPlan(
    val fromAlias: String,
    val toAlias: String,
)
