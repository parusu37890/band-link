package com.example.bandlink.matrix;

/** One immutable row from the generated release test inventory. */
public record ReleaseCase(
        String testId,
        String requirementId,
        String layer,
        String priority,
        String feature,
        String userState,
        String inputState,
        String dataState,
        String operation,
        String networkState,
        String expected,
        String dbCheck,
        String apiCheck,
        String uiCheck) {
}
