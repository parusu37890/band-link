package com.example.bandlink.dto;

/**
 * The decade an age falls in, which is as much as any public screen says about it.
 *
 * ProfileResponse worked this out inline; the listing needs the same answer next to the poster's
 * name, and two copies of the rule is exactly how a public screen ends up disagreeing with itself
 * about what it will reveal. PublicContractTest pins the behaviour.
 */
public final class AgeBand {
    private AgeBand() {}

    public static String of(Integer age) {
        return age == null ? null : (age / 10 * 10) + "代";
    }
}
