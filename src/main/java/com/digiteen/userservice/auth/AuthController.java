package com.digiteen.userservice.auth;

import com.digiteen.userservice.auth.dto.LoginRequest;
import com.digiteen.userservice.auth.dto.RefreshRequest;
import com.digiteen.userservice.auth.dto.RegisterRequest;
import com.digiteen.userservice.auth.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Registration, login, token rotation, and logout")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a user", description = "Creates an active user and returns a new token pair.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email or phone already exists", content = @Content)
    })
    ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authenticationService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in", description = "Authenticates by email or phone and returns a new token pair.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication succeeded"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
    })
    TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authenticationService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate a refresh token",
            description = "Revokes the submitted refresh token and returns a replacement token pair.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token rotated"),
            @ApiResponse(responseCode = "401", description = "Invalid, expired, or reused refresh token", content = @Content)
    })
    TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authenticationService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out", description = "Revokes the supplied refresh token. The operation is idempotent.")
    @ApiResponse(responseCode = "204", description = "Refresh token revoked")
    ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authenticationService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
