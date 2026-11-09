package br.com.ofisy.ms_ofisy_billing.shared.jwt;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JwtTestTokens {

    public static final String SECRET = "test-secret-key-minimum-256-bits-long-for-hs256-algorithm-ok";
    public static final String EMAIL = "mecanico@ofisy.com";
    public static final String OTHER_SECRET = "another-secret-key-minimum-256-bits-long-for-hs256-ok";
    public static final String CUSTOMER_ISSUER = "techchallenge-ofisy-auth";

    public static String withRole(String secret, String role) {
        return builder(secret, JwtService.ISSUER, Instant.now().plus(Duration.ofHours(1)))
                .claim(JwtService.ROLE_CLAIM, role)
                .compact();
    }

    public static String withoutRole(String secret) {
        return builder(secret, JwtService.ISSUER, Instant.now().plus(Duration.ofHours(1))).compact();
    }

    public static String expired(String secret, String role) {
        return builder(secret, JwtService.ISSUER, Instant.now().minus(Duration.ofMinutes(1)))
                .claim(JwtService.ROLE_CLAIM, role)
                .compact();
    }

    public static String withIssuer(String secret, String issuer, String role) {
        return builder(secret, issuer, Instant.now().plus(Duration.ofHours(1)))
                .claim(JwtService.ROLE_CLAIM, role)
                .compact();
    }

    private static JwtBuilder builder(String secret, String issuer, Instant expiration) {
        Instant issuedAt = expiration.minus(Duration.ofHours(2));
        return Jwts.builder()
                .issuer(issuer)
                .subject(EMAIL)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)));
    }
}
