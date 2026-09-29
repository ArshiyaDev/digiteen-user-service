package com.digiteen.userservice.security;

import com.nimbusds.jose.jwk.RSAKey;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwkController {

    private final JwtKeyProvider keys;

    public JwkController(JwtKeyProvider keys) {
        this.keys = keys;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        RSAKey publicJwk = new RSAKey.Builder(keys.publicKey())
                .keyID(keys.keyId())
                .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
                .build();
        return Map.of("keys", List.of(publicJwk.toJSONObject()));
    }
}
