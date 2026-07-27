package com.sharecutter.backend.security;

import com.sharecutter.backend.config.JwtProperties;
import com.sharecutter.backend.domain.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    private static final String USER_ID_CLAIM = "user_id";
    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_TYPE_CLAIM = "token_type";

    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateAccessToken(UserEntity user) {
        return generateToken(
                user,
                ACCESS_TOKEN_TYPE,
                jwtProperties.getAccessTokenExpiration().toSeconds()
        );
    }

    public String generateRefreshToken(UserEntity user) {
        return generateToken(
                user,
                REFRESH_TOKEN_TYPE,
                jwtProperties.getRefreshTokenExpiration().toSeconds()
        );
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public UUID extractUserId(String token) {
        String userId = extractClaim(
                token,
                claims -> claims.get(USER_ID_CLAIM, String.class)
        );

        return UUID.fromString(userId);
    }

    public String extractRole(String token) {
        return extractClaim(
                token,
                claims -> claims.get(ROLE_CLAIM, String.class)
        );
    }

    public String extractTokenType(String token) {
        return extractClaim(
                token,
                claims -> claims.get(TOKEN_TYPE_CLAIM, String.class)
        );
    }

    public boolean isAccessToken(String token) {
        return ACCESS_TOKEN_TYPE.equals(extractTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return REFRESH_TOKEN_TYPE.equals(extractTokenType(token));
    }

    public boolean isTokenValid(String token, UserEntity user) {
        try {
            String email = extractEmail(token);

            return email.equalsIgnoreCase(user.getEmail())
                    && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    private String generateToken(
            UserEntity user,
            String tokenType,
            long expirationSeconds
    ) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(expirationSeconds);

        return Jwts.builder()
                .subject(user.getEmail())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim(USER_ID_CLAIM, user.getId().toString())
                .claim(ROLE_CLAIM, user.getRole().name())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .signWith(signingKey)
                .compact();
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration.before(new Date());
    }

    private <T> T extractClaim(
            String token,
            Function<Claims, T> claimsResolver
    ) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(jwtProperties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}