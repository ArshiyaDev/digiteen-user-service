package com.digiteen.userservice.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Access and refresh token pair")
public record TokenResponse(
        @Schema(example = "Bearer") String tokenType,
        @Schema(description = "RS256 JSON Web Token") String accessToken,
        @Schema(description = "Access-token lifetime in seconds", example = "900") long expiresIn,
        @Schema(description = "Single-use rotating refresh token") String refreshToken) {
}
