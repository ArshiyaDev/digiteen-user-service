package com.digiteen.userservice.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.digiteen.userservice.auth.dto.LoginRequest;
import com.digiteen.userservice.auth.dto.RegisterRequest;
import com.digiteen.userservice.auth.dto.TokenResponse;
import com.digiteen.userservice.config.JwtProperties;
import com.digiteen.userservice.error.ApiException;
import com.digiteen.userservice.user.UserEntity;
import com.digiteen.userservice.user.UserRepository;
import com.digiteen.userservice.user.UserStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;

    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                "issuer", "audience", "local-audience", Duration.ofMinutes(15), Duration.ofDays(7),
                "private", "public", false);
        service = new AuthenticationService(
                userRepository, refreshTokenRepository,
                passwordEncoder, jwtService, properties);
    }

    @Test
    void registrationHashesPasswordBeforePersistence() {
        when(passwordEncoder.encode("correct horse battery staple")).thenReturn("$2a$12$hashed");
        when(jwtService.issueAccessToken(any())).thenReturn(new JwtService.IssuedAccessToken("access", 900));
        when(jwtService.newRefreshToken()).thenReturn("refresh");
        when(jwtService.hashRefreshToken("refresh")).thenReturn("a".repeat(64));

        TokenResponse response = service.register(new RegisterRequest(
                "Arshiya", "USER@Example.com", null, "correct horse battery staple"));

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        UserEntity saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("user@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("$2a$12$hashed");
        assertThat(saved.getPasswordHash()).doesNotContain("correct horse battery staple");

        assertThat(response.accessToken()).isEqualTo("access");
        verify(refreshTokenRepository).save(any(RefreshTokenEntity.class));
    }

    @Test
    void loginUsesAConstantPublicErrorForUnknownUsers() {
        when(userRepository.findByIdentifier("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("missing@example.com", "some-password")))
                .isInstanceOf(ApiException.class)
                .hasMessage("The supplied credentials are invalid");
    }

    @Test
    void loginRejectsAnIncorrectPassword() {
        UserEntity user = activeUser();
        when(userRepository.findByIdentifier("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("user@example.com", "wrong-password")))
                .isInstanceOf(ApiException.class)
                .hasMessage("The supplied credentials are invalid");
    }

    @Test
    void refreshTokenIsRotatedAndThePreviousTokenIsRevoked() {
        UserEntity user = activeUser();
        RefreshTokenEntity existing = new RefreshTokenEntity(
                UUID.randomUUID(), user, "o".repeat(64), Instant.now().plusSeconds(3600),
                Instant.now(), "trace-1");
        when(jwtService.hashRefreshToken("old-refresh")).thenReturn("o".repeat(64));
        when(refreshTokenRepository.findByTokenHash("o".repeat(64))).thenReturn(Optional.of(existing));
        when(jwtService.newRefreshToken()).thenReturn("new-refresh");
        when(jwtService.hashRefreshToken("new-refresh")).thenReturn("n".repeat(64));
        when(jwtService.issueAccessToken(user)).thenReturn(new JwtService.IssuedAccessToken("new-access", 900));

        TokenResponse response = service.refresh("old-refresh");

        assertThat(existing.getRevokedAt()).isNotNull();
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        verify(refreshTokenRepository).save(any(RefreshTokenEntity.class));
    }

    private UserEntity activeUser() {
        return new UserEntity(UUID.randomUUID(), "Arshiya", "user@example.com", null,
                "$2a$12$hashed", UserStatus.ACTIVE);
    }
}
