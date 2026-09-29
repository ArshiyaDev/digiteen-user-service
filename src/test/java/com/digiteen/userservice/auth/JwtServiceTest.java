package com.digiteen.userservice.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.digiteen.userservice.config.JwtProperties;
import com.digiteen.userservice.security.JwtKeyProvider;
import com.digiteen.userservice.user.UserEntity;
import com.digiteen.userservice.user.UserStatus;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtServiceTest {

    @Test
    void issuedTokenIsRsaSignedAndContainsOwnershipClaims() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) pair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) pair.getPrivate();

        JwtKeyProvider keys = org.mockito.Mockito.mock(JwtKeyProvider.class);
        when(keys.publicKey()).thenReturn(publicKey);
        when(keys.privateKey()).thenReturn(privateKey);
        when(keys.keyId()).thenReturn("test-key");

        RSAKey rsaKey = new RSAKey.Builder(publicKey).privateKey(privateKey).keyID("test-key").build();
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
        JwtProperties properties = new JwtProperties(
                "https://identity.digiteen.local", "digiteen-wallet-service", "digiteen-user-service",
                Duration.ofMinutes(15), Duration.ofDays(7), "private", "public", false);
        JwtService service = new JwtService(encoder, properties, keys);
        UserEntity user = new UserEntity(UUID.randomUUID(), "Test User", "test@example.com", null,
                "hash", UserStatus.ACTIVE);

        JwtService.IssuedAccessToken issued = service.issueAccessToken(user);
        Jwt decoded = NimbusJwtDecoder.withPublicKey(publicKey).build().decode(issued.value());

        assertThat(decoded.getSubject()).isEqualTo(user.getId().toString());
        assertThat(decoded.getIssuer().toString()).isEqualTo("https://identity.digiteen.local");
        assertThat(decoded.getAudience()).containsExactly("digiteen-user-service", "digiteen-wallet-service");
        assertThat(decoded.getClaimAsString("scope")).contains("wallet:read", "wallet:write");
        assertThat(issued.expiresInSeconds()).isEqualTo(900);
    }

    @Test
    void refreshTokensAreHighEntropyAndOnlyTheirHashNeedsPersistence() throws Exception {
        JwtService service = new JwtService(null, null, null);

        String first = service.newRefreshToken();
        String second = service.newRefreshToken();

        assertThat(first).hasSizeGreaterThan(80).isNotEqualTo(second);
        assertThat(service.hashRefreshToken(first)).hasSize(64).doesNotContain(first);
    }
}
