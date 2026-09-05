package com.example.bandlink.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class TokenRequests {
    private TokenRequests() {}
    public record VerifyEmailRequest(@NotBlank @Size(max = 100) String token) {}
    public record PasswordResetRequest(@NotBlank @Email @Size(max = 320) String email) {}
    public record PasswordResetConfirmRequest(@NotBlank @Size(max = 100) String token,
                                               @NotBlank @Size(min = 8, max = 128) String newPassword) {}
}
