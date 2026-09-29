package com.digiteen.userservice.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "New user registration")
public record RegisterRequest(
        @Schema(example = "Arshiya Example") @NotBlank @Size(max = 120) String name,
        @Schema(example = "arshiya@example.com") @Email @Size(max = 254) String email,
        @Schema(example = "+989121234567")
        @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "must be a valid international phone number") String phone,
        @Schema(example = "correct-horse-battery-staple", minLength = 10, maxLength = 72)
        @NotBlank @Size(min = 10, max = 72) String password) {

    @AssertTrue(message = "either email or phone must be provided")
    public boolean isContactProvided() {
        return email != null && !email.isBlank() || phone != null && !phone.isBlank();
    }
}
