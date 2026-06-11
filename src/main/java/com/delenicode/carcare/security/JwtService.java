package com.delenicode.carcare.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final JwtProperties properties;
  private final SecretKey key;

  public JwtService(JwtProperties properties) {
    this.properties = properties;
    this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
  }

  public String createAccessToken(String subject, Map<String, Object> claims) {
    return createToken(subject, claims, Instant.now().plusSeconds(properties.accessMinutes() * 60));
  }

  public String createRefreshToken(String subject) {
    return createToken(subject, Map.of("type", "refresh"), Instant.now().plusSeconds(properties.refreshDays() * 86400));
  }

  public String subject(String token) {
    return claims(token).getSubject();
  }

  public Instant expiresAt(String token) {
    return claims(token).getExpiration().toInstant();
  }

  public boolean isValid(String token) {
    claims(token);
    return true;
  }

  private String createToken(String subject, Map<String, Object> claims, Instant expiresAt) {
    return Jwts.builder()
        .subject(subject)
        .claims(claims)
        .issuedAt(Date.from(Instant.now()))
        .expiration(Date.from(expiresAt))
        .signWith(key)
        .compact();
  }

  private Claims claims(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
