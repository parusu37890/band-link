package com.example.bandlink.dto;

/** Public profile age formatting. The product now displays the entered age in years. */
public final class AgeBand {
    private AgeBand() {}

    public static String of(Integer age) {
        return age == null ? null : age + "歳";
    }
}
