package com.flowdeck.backend.presence.service;

import com.flowdeck.backend.permission.service.PermissionService;
import com.flowdeck.backend.presence.domain.ProjectPresenceSession;
import com.flowdeck.backend.presence.dto.ProjectPresenceMemberResponse;
import com.flowdeck.backend.presence.dto.ProjectPresenceResponse;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProjectPresenceService {

  private static final String PROJECT_PRESENCE_KEY_PREFIX = "presence:project:";
  private static final String SESSION_KEY_PREFIX = "ws:session:";
  private static final Duration SESSION_TTL = Duration.ofSeconds(30);
  private static final String UNKNOWN_USER_NAME = "알 수 없는 사용자";
  private static final String SESSION_FIELD_DELIMITER = "|";

  private final StringRedisTemplate stringRedisTemplate;
  private final PermissionService permissionService;
  private final UserRepository userRepository;

  public ProjectPresenceService(
      StringRedisTemplate stringRedisTemplate,
      PermissionService permissionService,
      UserRepository userRepository) {
    this.stringRedisTemplate = stringRedisTemplate;
    this.permissionService = permissionService;
    this.userRepository = userRepository;
  }

  public ProjectPresenceResponse getPresence(String projectId, Long userId) {
    permissionService.validateProjectAccess(projectId, userId);
    return snapshot(projectId);
  }

  public ProjectPresenceResponse join(String projectId, Long userId, String sessionId) {
    permissionService.validateProjectAccess(projectId, userId);
    refreshSession(projectId, userId, sessionId);
    return snapshot(projectId);
  }

  public ProjectPresenceResponse heartbeat(String projectId, Long userId, String sessionId) {
    permissionService.validateProjectAccess(projectId, userId);
    refreshSession(projectId, userId, sessionId);
    return snapshot(projectId);
  }

  public Optional<ProjectPresenceResponse> leave(String sessionId) {
    ProjectPresenceSession session = readSession(sessionId);
    if (session == null) {
      return Optional.empty();
    }

    stringRedisTemplate.opsForZSet().remove(projectPresenceKey(session.projectId()), sessionId);
    stringRedisTemplate.delete(sessionKey(sessionId));
    return Optional.of(snapshot(session.projectId()));
  }

  private void refreshSession(String projectId, Long userId, String sessionId) {
    ProjectPresenceSession existingSession = readSession(sessionId);
    if (existingSession != null && !projectId.equals(existingSession.projectId())) {
      stringRedisTemplate
          .opsForZSet()
          .remove(projectPresenceKey(existingSession.projectId()), sessionId);
    }

    Instant now = Instant.now();
    ProjectPresenceSession updatedSession = new ProjectPresenceSession(projectId, userId, now);

    stringRedisTemplate
        .opsForValue()
        .set(sessionKey(sessionId), writeSession(updatedSession), SESSION_TTL);
    stringRedisTemplate
        .opsForZSet()
        .add(projectPresenceKey(projectId), sessionId, now.toEpochMilli());
    pruneExpiredSessions(projectId, now);
  }

  private ProjectPresenceResponse snapshot(String projectId) {
    Instant now = Instant.now();
    pruneExpiredSessions(projectId, now);

    Set<String> sessionIds =
        stringRedisTemplate.opsForZSet().range(projectPresenceKey(projectId), 0, -1);
    if (sessionIds == null || sessionIds.isEmpty()) {
      return ProjectPresenceResponse.empty(projectId, now);
    }

    Map<Long, Integer> sessionCounts = new LinkedHashMap<>();
    Map<Long, Instant> lastSeenAtByUser = new LinkedHashMap<>();
    List<String> staleSessionIds = new ArrayList<>();

    for (String sessionId : sessionIds) {
      ProjectPresenceSession session = readSession(sessionId);
      if (session == null || !projectId.equals(session.projectId())) {
        staleSessionIds.add(sessionId);
        continue;
      }

      sessionCounts.merge(session.userId(), 1, Integer::sum);
      lastSeenAtByUser.compute(
          session.userId(),
          (ignored, previousLastSeenAt) ->
              previousLastSeenAt == null || session.lastSeenAt().isAfter(previousLastSeenAt)
                  ? session.lastSeenAt()
                  : previousLastSeenAt);
    }

    if (!staleSessionIds.isEmpty()) {
      stringRedisTemplate
          .opsForZSet()
          .remove(projectPresenceKey(projectId), staleSessionIds.toArray());
    }

    if (sessionCounts.isEmpty()) {
      return ProjectPresenceResponse.empty(projectId, now);
    }

    Map<Long, String> userNames = userNames(sessionCounts.keySet());
    List<ProjectPresenceMemberResponse> members =
        sessionCounts.entrySet().stream()
            .map(
                entry ->
                    new ProjectPresenceMemberResponse(
                        entry.getKey(),
                        userNames.getOrDefault(entry.getKey(), UNKNOWN_USER_NAME),
                        entry.getValue(),
                        lastSeenAtByUser.get(entry.getKey())))
            .sorted(
                Comparator.comparing(ProjectPresenceMemberResponse::lastSeenAt)
                    .reversed()
                    .thenComparing(ProjectPresenceMemberResponse::userName))
            .toList();

    return ProjectPresenceResponse.of(projectId, members, now);
  }

  private void pruneExpiredSessions(String projectId, Instant now) {
    double expiredBefore = now.minusMillis(SESSION_TTL.toMillis()).toEpochMilli();
    stringRedisTemplate
        .opsForZSet()
        .removeRangeByScore(projectPresenceKey(projectId), Double.NEGATIVE_INFINITY, expiredBefore);
  }

  private Map<Long, String> userNames(Set<Long> userIds) {
    return userRepository.findAllById(userIds).stream()
        .collect(
            java.util.stream.Collectors.toMap(User::getId, User::getName, (left, right) -> left));
  }

  private ProjectPresenceSession readSession(String sessionId) {
    String payload = stringRedisTemplate.opsForValue().get(sessionKey(sessionId));
    if (payload == null) {
      return null;
    }

    String[] parts = payload.split("\\|", -1);
    if (parts.length != 3) {
      throw new IllegalStateException("Failed to read project presence session.");
    }

    return new ProjectPresenceSession(
        parts[0], Long.parseLong(parts[1]), Instant.ofEpochMilli(Long.parseLong(parts[2])));
  }

  private String writeSession(ProjectPresenceSession session) {
    return session.projectId()
        + SESSION_FIELD_DELIMITER
        + session.userId()
        + SESSION_FIELD_DELIMITER
        + session.lastSeenAt().toEpochMilli();
  }

  private String projectPresenceKey(String projectId) {
    return PROJECT_PRESENCE_KEY_PREFIX + projectId;
  }

  private String sessionKey(String sessionId) {
    return SESSION_KEY_PREFIX + sessionId;
  }
}
