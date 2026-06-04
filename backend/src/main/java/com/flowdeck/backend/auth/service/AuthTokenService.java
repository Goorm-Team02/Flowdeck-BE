package com.flowdeck.backend.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthTokenService {

  private static final String REFRESH_KEY_PREFIX = "auth:refresh:";
  private static final String BLACKLIST_KEY_PREFIX = "auth:blacklist:";
  private static final String FORCE_LOGOUT_KEY_PREFIX = "auth:force-logout:";

  private final StringRedisTemplate stringRedisTemplate;

  public AuthTokenService(StringRedisTemplate stringRedisTemplate) {
    this.stringRedisTemplate = stringRedisTemplate;
  }

  public void saveRefreshToken(Long userId, String refreshToken, Duration ttl) {
    stringRedisTemplate.opsForValue().set(refreshKey(userId), hash(refreshToken), ttl);
  }

  public boolean matchesRefreshToken(Long userId, String refreshToken) {
    String savedTokenHash = stringRedisTemplate.opsForValue().get(refreshKey(userId));
    return hash(refreshToken).equals(savedTokenHash);
  }

  public void deleteRefreshToken(Long userId) {
    stringRedisTemplate.delete(refreshKey(userId));
  }

  public void blacklistAccessToken(String accessToken, Duration ttl) {
    stringRedisTemplate.opsForValue().set(blacklistKey(accessToken), "logout", ttl);
  }

  public boolean isBlacklisted(String accessToken) {
    return Boolean.TRUE.equals(stringRedisTemplate.hasKey(blacklistKey(accessToken)));
  }

  public void forceLogout(Long userId, Duration ttl) {
    stringRedisTemplate
        .opsForValue()
        .set(forceLogoutKey(userId), String.valueOf(Instant.now().toEpochMilli()), ttl);
  }

  public boolean isForceLogout(Long userId, long tokenIssuedAtMillis) {
    String forcedAtMillis = stringRedisTemplate.opsForValue().get(forceLogoutKey(userId));
    return forcedAtMillis != null && tokenIssuedAtMillis <= Long.parseLong(forcedAtMillis);
  }

  private String refreshKey(Long userId) {
    return REFRESH_KEY_PREFIX + userId;
  }

  private String blacklistKey(String accessToken) {
    return BLACKLIST_KEY_PREFIX + hash(accessToken);
  }

  private String forceLogoutKey(Long userId) {
    return FORCE_LOGOUT_KEY_PREFIX + userId;
  }

  private String hash(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashedToken = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashedToken);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 algorithm is not available.", exception);
    }
  }
}
