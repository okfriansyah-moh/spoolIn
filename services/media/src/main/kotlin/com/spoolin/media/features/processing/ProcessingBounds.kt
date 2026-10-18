package com.spoolin.media.features.processing

/**
 * Source-prescribed processing limits for artwork validation and re-encoding (§7.6 / OD-007 / OD-019).
 *
 * Applying each limit independently prevents a file that passes one check from bypassing another.
 * These are engineering defaults; configuration adjustments follow OD-020 via PolicySettings.
 */
object ProcessingBounds {
    /** Minimum accepted input stream bytes (exact transferred content, §7.6). */
    const val MIN_INPUT_BYTES: Long = 1L

    /** Maximum accepted input stream bytes — 5 MiB (AC-149, PRD-POLICY-28). */
    const val MAX_INPUT_BYTES: Long = 5_242_880L

    /** Maximum decoded dimension in either axis — px (§7.6). */
    const val MAX_DIMENSION_PX: Int = 8192

    /** Bounded header helper output, in bytes. */
    const val MAX_HEADER_BYTES: Int = 64

    /** Maximum supported decoded color channel count. */
    const val MAX_COLOR_BANDS: Int = 4

    /** Maximum decoded total pixels (§7.6). */
    const val MAX_PIXELS: Long = 40_000_000L

    /** Maximum decoded raw pixel budget — bytes (§7.6). */
    const val MAX_DECODED_BYTES: Long = 160_000_000L

    /** Maximum subprocess resident set size — MiB (§7.6). */
    const val MAX_RSS_MIB: Long = 256L

    /** Maximum CPU time for the processor subprocess — seconds (§7.6). */
    const val MAX_CPU_SECONDS: Long = 5L

    /** Maximum wall-clock time for the processor subprocess — seconds (§7.6). */
    const val MAX_WALL_SECONDS: Long = 10L

    /** Maximum temporary disk usage — MiB (§7.6). */
    const val MAX_TEMP_MIB: Long = 32L

    /** Maximum re-encoded output size — bytes / 8 MiB (§7.6). */
    const val MAX_OUTPUT_BYTES: Long = 8_388_608L

    /** Accepted static image formats. Animated variants are rejected (AC-150). */
    val SUPPORTED_FORMATS: Set<String> = setOf("JPEG", "PNG", "WEBP")

    /** 64 KiB transfer chunk size for Core→Media gRPC stream (§5.3). */
    const val CHUNK_BYTES: Int = 65_536

    /** Maximum single gRPC message size (§5.3). */
    const val MAX_MESSAGE_BYTES: Int = 131_072 // 128 KiB

    /** Quarantine path name for staging files — UUID only, no user input (§7.6 / §8.3.2). */
    const val STAGING_SUBDIR: String = "quarantine"

    /** Output path name for processed READY files — UUID only. */
    const val OUTPUT_SUBDIR: String = "output"
}
