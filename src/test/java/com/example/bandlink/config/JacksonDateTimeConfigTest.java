package com.example.bandlink.config;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression: LocalDateTime.now(Clock.systemDefaultZone()) in this app is always a UTC instant
 * (the container's system zone is UTC), but the type itself carries no zone. The default Jackson
 * serialization wrote microsecond precision with no zone suffix, which is not the strict
 * ECMAScript Date Time String Format - browsers fell back to a lenient parse and, on a JST client,
 * silently read it as local time instead of UTC. A post created seconds ago showed as "9時間前".
 */
class JacksonDateTimeConfigTest {
    @Test
    void localDateTimeSerializesAsMillisecondPrecisionUtc() {
        JsonMapper.Builder builder = JsonMapper.builder();
        new JacksonDateTimeConfig().utcLocalDateTimeSerialization().customize(builder);
        JsonMapper mapper = builder.build();
        String json = mapper.writeValueAsString(LocalDateTime.of(2026, 9, 15, 15, 33, 23, 928_159_000));
        assertEquals("\"2026-09-15T15:33:23.928Z\"", json);
        // The exact bug: a browser's Date parser reads a bare 6-fraction-digit string as local
        // time on a non-UTC system clock. Confirm the fixed format matches the strict subset the
        // spec always parses as UTC (T, exactly SSS, then Z) rather than falling into the lenient
        // fallback path.
        assertTrue(json.matches("\"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z\""),
                () -> "unexpected format: " + json);
    }
}
