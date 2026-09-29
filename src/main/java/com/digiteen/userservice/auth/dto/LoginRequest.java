package com.digiteen.userservice.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Email/phone login credentials")
public record LoginRequest(
        @Schema(example = "arshiya@example.com") @NotBlank @Size(max = 254) String identifier,
        @Schema(example = "correct-horse-battery-staple") @NotBlank @Size(max = 72) String password) {
}
