package com.flowdeck.backend.auth.service;

import java.time.Duration;
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
    stringRedisTemplate.opsForValue().set(refreshKey(userId), refreshToken, ttl);
  }

  public boolean matchesRefreshToken(Long userId, String refreshToken) {
    String savedToken = stringRedisTemplate.opsForValue().get(refreshKey(userId));
    return refreshToken.equals(savedToken);
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
    stringRedisTemplate.opsForValue().set(forceLogoutKey(userId), "1", ttl);
  }

  public boolean isForceLogout(Long userId) {
    return Boolean.TRUE.equals(stringRedisTemplate.hasKey(forceLogoutKey(userId)));
  }

  private String refreshKey(Long userId) {
    return REFRESH_KEY_PREFIX + userId;
  }

  private String blacklistKey(String accessToken) {
    return BLACKLIST_KEY_PREFIX + accessToken;
  }

  private String forceLogoutKey(Long userId) {
    return FORCE_LOGOUT_KEY_PREFIX + userId;
  }
}
