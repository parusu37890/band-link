package com.example.bandlink.config;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * Every LocalDateTime in this app comes from Clock.systemDefaultZone(), which is UTC in the
 * container, so a LocalDateTime here always genuinely represents a UTC instant even though the
 * type itself carries no zone. The default serialization wrote microsecond precision with no zone
 * suffix (e.g. "2026-09-15T15:33:23.928159"), which does not match the strict ECMAScript Date Time
 * String Format (exactly 3 fractional digits, or a zone). Browsers fall back to a lenient,
 * implementation-defined parse for a string that loose, and on a JST system clock silently read it
 * as local time instead of UTC - every relative-time label on the site (post age, last login, ...)
 * was showing a brand-new post as "9時間前". Truncating to milliseconds and appending Z removes the
 * ambiguity.
 */
@Configuration
public class JacksonDateTimeConfig {
    private static final DateTimeFormatter UTC_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Bean
    public JsonMapperBuilderCustomizer utcLocalDateTimeSerialization() {
        return builder -> builder.addModule(new SimpleModule()
                .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(UTC_MILLIS)));
    }
}
