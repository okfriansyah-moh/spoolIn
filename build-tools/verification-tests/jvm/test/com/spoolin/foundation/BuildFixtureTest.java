package com.spoolin.foundation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.spoolin.foundation.fixture.BuildRoundTrip;
import org.junit.jupiter.api.Test;

class BuildFixtureTest {
    @Test
    void generatedProtobufCompilesAndPreservesUnicode() {
        assertEquals("fixture 🎬", BuildRoundTrip.INSTANCE.roundTrip("fixture 🎬"));
    }
}
