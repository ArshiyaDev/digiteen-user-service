package com.digiteen.userservice.auth;

import com.digiteen.userservice.auth.dto.LoginRequest;
import com.digiteen.userservice.auth.dto.RegisterRequest;
import com.digiteen.userservice.auth.dto.TokenResponse;
import com.digiteen.userservice.config.JwtProperties;
import com.digiteen.userservice.error.ApiException;
import com.digiteen.userservice.observability.TraceContext;
import com.digiteen.userservice.user.UserEntity;
import com.digiteen.userservice.user.UserRepository;
import com.digiteen.userservice.user.UserStatus;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthenticationService(UserRepository userRepository,
                                 RefreshTokenRepository refreshTokenRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtService jwtService,
                                 JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        String phone = normalizePhone(request.phone());
        ensureContactIsAvailable(email, phone);

        UserEntity user = new UserEntity(
                UUID.randomUUID(),
                request.name().trim(),
                email,
                phone,
                passwordEncoder.encode(request.password()),
                UserStatus.ACTIVE);
        userRepository.save(user);

        log.atInfo()
                .addKeyValue("event", "user_registered")
                .addKeyValue("userId", user.getId())
                .log("User registered");
        return createSession(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        UserEntity user = userRepository.findByIdentifier(identifier)
                .orElseThrow(this::invalidCredentials);

        if (user.getStatus() != UserStatus.ACTIVE
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.atWarn()
                    .addKeyValue("event", "login_failed")
                    .addKeyValue("userId", user.getId())
                    .log("Login failed");
            throw invalidCredentials();
        }

        log.atInfo()
                .addKeyValue("event", "login_succeeded")
                .addKeyValue("userId", user.getId())
                .log("Login succeeded");
        return createSession(user);
    }

    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        Instant now = Instant.now();
        String tokenHash = jwtService.hashRefreshToken(rawRefreshToken);
        RefreshTokenEntity existing = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(this::invalidRefreshToken);

        if (existing.getRevokedAt() != null) {
            refreshTokenRepository.findAllByUser_IdAndRevokedAtIsNull(existing.getUser().getId())
                    .forEach(token -> token.revoke(now, null));
            log.atWarn()
                    .addKeyValue("event", "refresh_token_reuse_detected")
                    .addKeyValue("userId", existing.getUser().getId())
                    .log("A rotated refresh token was reused; active sessions were revoked");
            throw invalidRefreshToken();
        }

        if (!existing.isUsableAt(now) || existing.getUser().getStatus() != UserStatus.ACTIVE) {
            throw invalidRefreshToken();
        }

        String replacement = jwtService.newRefreshToken();
        String replacementHash = jwtService.hashRefreshToken(replacement);
        existing.revoke(now, replacementHash);
        storeRefreshToken(existing.getUser(), replacementHash, now);

        JwtService.IssuedAccessToken access = jwtService.issueAccessToken(existing.getUser());
        log.atInfo()
                .addKeyValue("event", "token_refreshed")
                .addKeyValue("userId", existing.getUser().getId())
                .log("Access token refreshed");
        return new TokenResponse("Bearer", access.value(), access.expiresInSeconds(), replacement);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        String tokenHash = jwtService.hashRefreshToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.revoke(Instant.now(), null);
                log.atInfo()
                        .addKeyValue("event", "user_logged_out")
                        .addKeyValue("userId", token.getUser().getId())
                        .log("User logged out");
            }
        });
    }

    private TokenResponse createSession(UserEntity user) {
        JwtService.IssuedAccessToken access = jwtService.issueAccessToken(user);
        String refreshToken = jwtService.newRefreshToken();
        storeRefreshToken(user, jwtService.hashRefreshToken(refreshToken), Instant.now());
        return new TokenResponse("Bearer", access.value(), access.expiresInSeconds(), refreshToken);
    }

    private void storeRefreshToken(UserEntity user, String tokenHash, Instant issuedAt) {
        RefreshTokenEntity refreshToken = new RefreshTokenEntity(
                UUID.randomUUID(), user, tokenHash,
                issuedAt.plus(jwtProperties.refreshTokenTtl()), issuedAt,
                TraceContext.currentTraceId());
        refreshTokenRepository.save(refreshToken);
    }

    private void ensureContactIsAvailable(String email, String phone) {
        if (email != null && userRepository.existsByEmailIgnoreCase(email)
                || phone != null && userRepository.existsByPhone(phone)) {
            throw new ApiException(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS",
                    "A user with the supplied email or phone already exists");
        }
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        return phone == null || phone.isBlank() ? null : phone.trim();
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                "The supplied credentials are invalid");
    }

    private ApiException invalidRefreshToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN",
                "The refresh token is invalid or expired");
    }
}
