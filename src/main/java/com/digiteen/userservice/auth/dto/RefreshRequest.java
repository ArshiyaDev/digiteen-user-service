package com.digiteen.userservice.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Opaque refresh token request")
public record RefreshRequest(
        @Schema(description = "Refresh token returned by login, register, or refresh", example = "opaque-refresh-token")
        @NotBlank @Size(max = 512) String refreshToken) {
}
