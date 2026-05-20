package com.flowdeck.backend.unit.presence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.flowdeck.backend.presence.domain.ProjectPresenceSession;
import com.flowdeck.backend.presence.store.RedisProjectPresenceStore;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

class RedisProjectPresenceStoreTest {

  private StringRedisTemplate stringRedisTemplate;
  private ValueOperations<String, String> valueOperations;
  private ZSetOperations<String, String> zSetOperations;
  private RedisProjectPresenceStore redisProjectPresenceStore;

  private Map<String, String> values;
  private Map<String, Map<String, Double>> zSets;

  @BeforeEach
  void setUp() {
    stringRedisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
    valueOperations = org.mockito.Mockito.mock(ValueOperations.class);
    zSetOperations = org.mockito.Mockito.mock(ZSetOperations.class);

    values = new HashMap<>();
    zSets = new HashMap<>();

    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
    when(stringRedisTemplate.delete(any(String.class)))
        .thenAnswer(
            invocation -> {
              values.remove(invocation.getArgument(0));
              return true;
            });
    when(valueOperations.get(any(String.class)))
        .thenAnswer(invocation -> values.get(invocation.getArgument(0)));
    org.mockito.Mockito.doAnswer(
            invocation -> {
              values.put(invocation.getArgument(0), invocation.getArgument(1));
              return null;
            })
        .when(valueOperations)
        .set(any(String.class), any(String.class), any(Duration.class));
    when(zSetOperations.add(any(String.class), any(String.class), any(Double.class)))
        .thenAnswer(
            invocation -> {
              String key = invocation.getArgument(0);
              String member = invocation.getArgument(1);
              Double score = invocation.getArgument(2);
              zSets.computeIfAbsent(key, ignored -> new HashMap<>()).put(member, score);
              return true;
            });
    when(zSetOperations.range(any(String.class), any(Long.class), any(Long.class)))
        .thenAnswer(
            invocation -> {
              String key = invocation.getArgument(0);
              Map<String, Double> members = zSets.getOrDefault(key, Map.of());
              List<Map.Entry<String, Double>> sortedMembers = new ArrayList<>(members.entrySet());
              sortedMembers.sort(Map.Entry.comparingByValue());
              LinkedHashSet<String> ordered = new LinkedHashSet<>();
              sortedMembers.forEach(entry -> ordered.add(entry.getKey()));
              return ordered;
            });
    when(zSetOperations.remove(any(String.class), any()))
        .thenAnswer(
            invocation -> {
              String key = invocation.getArgument(0);
              Object[] members =
                  invocation.getArguments().length > 1
                      ? java.util.Arrays.copyOfRange(
                          invocation.getArguments(), 1, invocation.getArguments().length)
                      : new Object[0];
              Map<String, Double> currentMembers = zSets.getOrDefault(key, Map.of());
              long removedCount = 0;
              for (Object member : members) {
                if (currentMembers.remove(member) != null) {
                  removedCount += 1;
                }
              }
              return removedCount;
            });
    when(zSetOperations.removeRangeByScore(any(String.class), any(Double.class), any(Double.class)))
        .thenAnswer(
            invocation -> {
              String key = invocation.getArgument(0);
              Double min = invocation.getArgument(1);
              Double max = invocation.getArgument(2);
              Map<String, Double> members = zSets.getOrDefault(key, Map.of());
              List<String> expiredMembers = new ArrayList<>();
              members.forEach(
                  (member, score) -> {
                    if (score >= min && score <= max) {
                      expiredMembers.add(member);
                    }
                  });
              expiredMembers.forEach(members::remove);
              return (long) expiredMembers.size();
            });

    redisProjectPresenceStore = new RedisProjectPresenceStore(stringRedisTemplate);
  }

  @Test
  void touchSessionStoresSerializedSessionWithoutUserName() {
    Instant now = Instant.now();

    redisProjectPresenceStore.touchSession(
        "session-1", new ProjectPresenceSession("project-123", 7L, now));

    assertThat(values.get("ws:session:session-1")).contains("project-123|7|");
    assertThat(values.get("ws:session:session-1")).doesNotContain("owner");
    assertThat(zSets.get("presence:project:project-123")).containsKey("session-1");
  }

  @Test
  void findActiveSessionsRemovesStaleSessionIds() {
    Instant now = Instant.now();
    values.put("ws:session:session-1", "project-123|7|" + now.minusSeconds(1).toEpochMilli());
    zSets.put(
        "presence:project:project-123",
        new HashMap<>(
            Map.of(
                "session-1", (double) now.minusSeconds(1).toEpochMilli(),
                "stale-session", (double) now.toEpochMilli())));

    List<ProjectPresenceSession> sessions =
        redisProjectPresenceStore.findActiveSessions("project-123", now);

    assertThat(sessions).singleElement().extracting(ProjectPresenceSession::userId).isEqualTo(7L);
    assertThat(zSets.get("presence:project:project-123")).doesNotContainKey("stale-session");
  }

  @Test
  void removeSessionDeletesSessionAndProjectIndex() {
    Instant now = Instant.now();
    values.put("ws:session:session-1", "project-123|7|" + now.toEpochMilli());
    zSets.put(
        "presence:project:project-123",
        new HashMap<>(Map.of("session-1", (double) now.toEpochMilli())));

    redisProjectPresenceStore.removeSession("session-1");

    assertThat(values).doesNotContainKey("ws:session:session-1");
    assertThat(zSets.get("presence:project:project-123")).doesNotContainKey("session-1");
  }
}
