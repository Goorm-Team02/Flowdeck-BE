package com.flowdeck.backend.presence.store;

import com.flowdeck.backend.presence.domain.ProjectPresenceSession;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisProjectPresenceStore implements ProjectPresenceStore {

  private static final String PROJECT_PRESENCE_KEY_PREFIX = "presence:project:";
  private static final String SESSION_KEY_PREFIX = "ws:session:";
  private static final Duration SESSION_TTL = Duration.ofSeconds(30);
  private static final String SESSION_FIELD_DELIMITER = "|";

  private final StringRedisTemplate stringRedisTemplate;

  public RedisProjectPresenceStore(StringRedisTemplate stringRedisTemplate) {
    this.stringRedisTemplate = stringRedisTemplate;
  }

  @Override
  public void touchSession(String sessionId, ProjectPresenceSession session) {
    findSession(sessionId)
        .filter(existingSession -> !session.projectId().equals(existingSession.projectId()))
        .ifPresent(
            existingSession ->
                stringRedisTemplate
                    .opsForZSet()
                    .remove(projectPresenceKey(existingSession.projectId()), sessionId));

    stringRedisTemplate
        .opsForValue()
        .set(sessionKey(sessionId), writeSession(session), SESSION_TTL);
    stringRedisTemplate
        .opsForZSet()
        .add(
            projectPresenceKey(session.projectId()),
            sessionId,
            session.lastSeenAt().toEpochMilli());
    pruneExpiredSessions(session.projectId(), session.lastSeenAt());
  }

  @Override
  public Optional<ProjectPresenceSession> findSession(String sessionId) {
    String payload = stringRedisTemplate.opsForValue().get(sessionKey(sessionId));
    if (payload == null) {
      return Optional.empty();
    }

    String[] parts = payload.split("\\|", -1);
    if (parts.length != 3) {
      throw new IllegalStateException("Failed to read project presence session.");
    }

    return Optional.of(
        new ProjectPresenceSession(
            parts[0], Long.parseLong(parts[1]), Instant.ofEpochMilli(Long.parseLong(parts[2]))));
  }

  @Override
  public void removeSession(String sessionId) {
    findSession(sessionId)
        .ifPresent(
            session -> {
              stringRedisTemplate
                  .opsForZSet()
                  .remove(projectPresenceKey(session.projectId()), sessionId);
              stringRedisTemplate.delete(sessionKey(sessionId));
            });
  }

  @Override
  public List<ProjectPresenceSession> findActiveSessions(String projectId, Instant now) {
    pruneExpiredSessions(projectId, now);

    Set<String> sessionIds =
        stringRedisTemplate.opsForZSet().range(projectPresenceKey(projectId), 0, -1);
    if (sessionIds == null || sessionIds.isEmpty()) {
      return List.of();
    }

    List<String> staleSessionIds = new ArrayList<>();
    List<ProjectPresenceSession> sessions = new ArrayList<>();

    for (String sessionId : sessionIds) {
      Optional<ProjectPresenceSession> session = findSession(sessionId);
      if (session.isEmpty()) {
        staleSessionIds.add(sessionId);
        continue;
      }

      ProjectPresenceSession activeSession = session.orElseThrow();
      if (!projectId.equals(activeSession.projectId())) {
        staleSessionIds.add(sessionId);
        continue;
      }

      sessions.add(activeSession);
    }

    if (!staleSessionIds.isEmpty()) {
      stringRedisTemplate
          .opsForZSet()
          .remove(projectPresenceKey(projectId), staleSessionIds.toArray());
    }

    return sessions;
  }

  private void pruneExpiredSessions(String projectId, Instant now) {
    double expiredBefore = now.minusMillis(SESSION_TTL.toMillis()).toEpochMilli();
    stringRedisTemplate
        .opsForZSet()
        .removeRangeByScore(projectPresenceKey(projectId), Double.NEGATIVE_INFINITY, expiredBefore);
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
