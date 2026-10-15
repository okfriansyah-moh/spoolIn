package com.spoolin.foundation.fixture

object BuildRoundTrip {
    fun roundTrip(value: String): String =
        BuildProbe
            .parseFrom(
                BuildProbe
                    .newBuilder()
                    .setValue(value)
                    .build()
                    .toByteArray(),
            ).value
}
