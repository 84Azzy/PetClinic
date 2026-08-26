package com.zzy.petclinic.authentication;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
  private final SecretKey key;
  private final Duration expiration;

  public JwtTokenProvider(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expiration = Duration.ofMinutes(expirationMinutes);
  }

  public String generate(AuthenticatedUser user) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(user.getUsername())
        .claim("uid", user.getId())
        .claim("tokenVersion", user.getTokenVersion())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(expiration)))
        .signWith(key)
        .compact();
  }

  public String getUsername(String token) {
    return parse(token).getSubject();
  }

  public boolean isValid(String token, AuthenticatedUser user) {
    Claims claims = parse(token);
    Integer tokenVersion = claims.get("tokenVersion", Integer.class);
    return user.getUsername().equals(claims.getSubject())
        && user.isEnabled()
        && tokenVersion != null
        && tokenVersion == user.getTokenVersion();
  }

  Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  public long expiresInSeconds() {
    return expiration.toSeconds();
  }
}
