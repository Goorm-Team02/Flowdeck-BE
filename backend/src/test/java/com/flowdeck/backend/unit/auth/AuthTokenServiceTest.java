package com.flowdeck.backend.unit.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flowdeck.backend.auth.service.AuthTokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class AuthTokenServiceTest {

  private static final Long USER_ID = 1L;
  private static final String REFRESH_TOKEN = "refresh-token";
  private static final String ACCESS_TOKEN = "access-token";

  private StringRedisTemplate stringRedisTemplate;
  private ValueOperations<String, String> valueOperations;
  private AuthTokenService authTokenService;

  @BeforeEach
  void setUp() {
    stringRedisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
    valueOperations = org.mockito.Mockito.mock(ValueOperations.class);
    authTokenService = new AuthTokenService(stringRedisTemplate);

    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
  }

  @Test
  void saveRefreshTokenStoresTokenHash() {
    Duration ttl = Duration.ofDays(14);

    authTokenService.saveRefreshToken(USER_ID, REFRESH_TOKEN, ttl);

    verify(valueOperations).set("auth:refresh:" + USER_ID, sha256(REFRESH_TOKEN), ttl);
  }

  @Test
  void matchesRefreshTokenComparesTokenHash() {
    when(valueOperations.get("auth:refresh:" + USER_ID)).thenReturn(sha256(REFRESH_TOKEN));

    boolean matches = authTokenService.matchesRefreshToken(USER_ID, REFRESH_TOKEN);

    assertThat(matches).isTrue();
  }

  @Test
  void matchesRefreshTokenReturnsFalseWhenHashDoesNotMatch() {
    when(valueOperations.get("auth:refresh:" + USER_ID)).thenReturn(sha256("other-token"));

    boolean matches = authTokenService.matchesRefreshToken(USER_ID, REFRESH_TOKEN);

    assertThat(matches).isFalse();
  }

  @Test
  void blacklistAccessTokenUsesTokenHashAsKey() {
    Duration ttl = Duration.ofMinutes(30);

    authTokenService.blacklistAccessToken(ACCESS_TOKEN, ttl);

    verify(valueOperations).set("auth:blacklist:" + sha256(ACCESS_TOKEN), "logout", ttl);
  }

  @Test
  void isBlacklistedChecksTokenHashKey() {
    when(stringRedisTemplate.hasKey("auth:blacklist:" + sha256(ACCESS_TOKEN))).thenReturn(true);

    boolean blacklisted = authTokenService.isBlacklisted(ACCESS_TOKEN);

    assertThat(blacklisted).isTrue();
  }

  private String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 algorithm is not available.", exception);
    }
  }
}
