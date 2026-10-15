package com.spoolin.foundation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.spoolin.foundation.fixture.BuildRoundTrip;
import com.spoolin.foundation.fixture.FixtureValue;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.module.kotlin.KotlinModule;

class BuildFixtureTest {
    @Test
    void generatedProtobufCompilesAndPreservesUnicode() {
        assertEquals("fixture 🎬", BuildRoundTrip.INSTANCE.roundTrip("fixture 🎬"));
    }

    @Test
    void patchedJacksonAndKotlinMetadataRoundTripAnImmutableValue() {
        var mapper = JsonMapper.builder().addModule(new KotlinModule.Builder().build()).build();
        var input = new FixtureValue("fixture 🎬");
        assertEquals(input, mapper.readValue(mapper.writeValueAsString(input), FixtureValue.class));
    }
}
