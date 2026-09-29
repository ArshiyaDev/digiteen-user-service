package com.digiteen.userservice.auth;

import com.digiteen.userservice.config.JwtProperties;
import com.digiteen.userservice.security.JwtKeyProvider;
import com.digiteen.userservice.user.UserEntity;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final List<String> USER_SCOPES = List.of("wallet:read", "wallet:write");

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final JwtKeyProvider keys;

    public JwtService(JwtEncoder encoder, JwtProperties properties, JwtKeyProvider keys) {
        this.encoder = encoder;
        this.properties = properties;
        this.keys = keys;
    }

    public IssuedAccessToken issueAccessToken(UserEntity user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(keys.keyId())
                .build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .audience(List.of(properties.localAudience(), properties.audience()))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("scope", String.join(" ", USER_SCOPES))
                .claim("name", user.getName())
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedAccessToken(token, properties.accessTokenTtl().toSeconds());
    }

    public String newRefreshToken() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashRefreshToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public record IssuedAccessToken(String value, long expiresInSeconds) {
    }
}
