package com.flowdeck.backend.unit.presence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flowdeck.backend.permission.service.PermissionService;
import com.flowdeck.backend.presence.dto.ProjectPresenceMemberResponse;
import com.flowdeck.backend.presence.dto.ProjectPresenceResponse;
import com.flowdeck.backend.presence.service.ProjectPresenceService;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

class ProjectPresenceServiceTest {

  private static final String PROJECT_ID = "project-123";

  private StringRedisTemplate stringRedisTemplate;
  private ValueOperations<String, String> valueOperations;
  private ZSetOperations<String, String> zSetOperations;
  private PermissionService permissionService;
  private UserRepository userRepository;
  private ProjectPresenceService projectPresenceService;

  private Map<String, String> values;
  private Map<String, Map<String, Double>> zSets;

  @BeforeEach
  void setUp() {
    stringRedisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
    valueOperations = org.mockito.Mockito.mock(ValueOperations.class);
    zSetOperations = org.mockito.Mockito.mock(ZSetOperations.class);
    permissionService = org.mockito.Mockito.mock(PermissionService.class);
    userRepository = org.mockito.Mockito.mock(UserRepository.class);

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

    projectPresenceService =
        new ProjectPresenceService(stringRedisTemplate, permissionService, userRepository);
  }

  @Test
  void joinStoresSessionWithoutSavingUserNameInRedis() {
    when(userRepository.findAllById(any())).thenReturn(List.of(user(7L, "owner")));

    ProjectPresenceResponse response = projectPresenceService.join(PROJECT_ID, 7L, "session-1");

    verify(permissionService).validateProjectAccess(PROJECT_ID, 7L);
    assertThat(response.connectedCount()).isEqualTo(1);
    assertThat(response.members())
        .singleElement()
        .extracting(ProjectPresenceMemberResponse::userName)
        .isEqualTo("owner");
    assertThat(values.get("ws:session:session-1")).contains(PROJECT_ID + "|7|");
    assertThat(values.get("ws:session:session-1")).doesNotContain("owner");
  }

  @Test
  void getPresenceGroupsMultipleSessionsByUser() throws Exception {
    Instant now = Instant.now();
    values.put("ws:session:session-1", sessionJson(PROJECT_ID, 7L, now.minusSeconds(5)));
    values.put("ws:session:session-2", sessionJson(PROJECT_ID, 7L, now));
    values.put("ws:session:session-3", sessionJson(PROJECT_ID, 8L, now.minusSeconds(2)));
    zSets.put(
        "presence:project:" + PROJECT_ID,
        new HashMap<>(
            Map.of(
                "session-1", (double) now.minusSeconds(5).toEpochMilli(),
                "session-2", (double) now.toEpochMilli(),
                "session-3", (double) now.minusSeconds(2).toEpochMilli())));
    when(userRepository.findAllById(any()))
        .thenReturn(List.of(user(7L, "owner"), user(8L, "editor")));

    ProjectPresenceResponse response = projectPresenceService.getPresence(PROJECT_ID, 1L);

    assertThat(response.connectedCount()).isEqualTo(2);
    assertThat(response.members())
        .extracting(ProjectPresenceMemberResponse::userName)
        .containsExactly("owner", "editor");
    assertThat(response.members().getFirst().sessionCount()).isEqualTo(2);
    assertThat(response.members().getFirst().userId()).isEqualTo(7L);
  }

  @Test
  void leaveRemovesSessionAndReturnsUpdatedPresence() throws Exception {
    Instant now = Instant.now();
    values.put("ws:session:session-1", sessionJson(PROJECT_ID, 7L, now));
    values.put("ws:session:session-2", sessionJson(PROJECT_ID, 8L, now.minusSeconds(1)));
    zSets.put(
        "presence:project:" + PROJECT_ID,
        new HashMap<>(
            Map.of(
                "session-1", (double) now.toEpochMilli(),
                "session-2", (double) now.minusSeconds(1).toEpochMilli())));
    when(userRepository.findAllById(any())).thenReturn(List.of(user(8L, "editor")));

    Optional<ProjectPresenceResponse> response = projectPresenceService.leave("session-1");

    assertThat(response).isPresent();
    assertThat(response.orElseThrow().connectedCount()).isEqualTo(1);
    assertThat(response.orElseThrow().members())
        .singleElement()
        .extracting(ProjectPresenceMemberResponse::userName)
        .isEqualTo("editor");
    assertThat(values).doesNotContainKey("ws:session:session-1");
    assertThat(zSets.get("presence:project:" + PROJECT_ID)).doesNotContainKey("session-1");
  }

  private String sessionJson(String projectId, Long userId, Instant lastSeenAt) throws Exception {
    return projectId + "|" + userId + "|" + lastSeenAt.toEpochMilli();
  }

  private User user(Long id, String name) {
    User user = new User(name + "@test.com", "password", name);
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }
}
