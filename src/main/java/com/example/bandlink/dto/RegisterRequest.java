package com.example.bandlink.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record RegisterRequest(
        @NotBlank @Size(max = 80) String username,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotNull @Min(0) @Max(120) Integer age,
        @NotNull @Min(0) @Max(100) Integer experienceYears,
        @NotNull @Pattern(regexp = "男|女", message = "性別は男または女から選択してください。") String gender,
        @NotEmpty Set<Long> partIds, @NotEmpty Set<Long> genreIds,
        @NotEmpty Set<Long> stanceIds, @NotEmpty Set<Long> prefectureIds
) {
    /** Backwards-compatible constructor for service-level tests and old callers. */
    public RegisterRequest(String username, String email, String password) {
        this(username, email, password, null, null, null, Set.of(), Set.of(), Set.of(), Set.of());
    }
}
