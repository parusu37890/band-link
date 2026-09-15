package com.example.bandlink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FeedbackRequest(@NotBlank @Size(max = 1000) String message,
                              @Size(max = 1000) String imageUrl) {}
