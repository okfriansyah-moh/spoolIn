package com.spoolin.catalog.seed

internal object SeedLimits {
    const val SCHEMA_VERSION = 2
    const val MOVIES = 100
    const val SERIES = 100
    const val TITLES = MOVIES + SERIES
    const val LABEL_CODE_POINTS = 255
    const val SYNOPSIS_CODE_POINTS = 5000
    const val GENRES_PER_TITLE = 20
    const val MAXIMUM_YEAR = 9999
    const val MONTHS_PER_YEAR = 12
}
