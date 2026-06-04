package com.flowdeck.backend.fileediting.store;

import com.flowdeck.backend.fileediting.domain.FileEditingSession;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisFileEditingStore implements FileEditingStore {

  private static final String FILE_EDITING_KEY_PREFIX = "presence:file:";
  private static final Duration SESSION_TTL = Duration.ofSeconds(30);
  private static final String USER_ID_FIELD = "userId";
  private static final String USER_NAME_FIELD = "userName";
  private static final String SESSION_ID_FIELD = "sessionId";
  private static final String LAST_SEEN_AT_FIELD = "lastSeenAt";

  private final StringRedisTemplate stringRedisTemplate;

  public RedisFileEditingStore(StringRedisTemplate stringRedisTemplate) {
    this.stringRedisTemplate = stringRedisTemplate;
  }

  @Override
  public Optional<FileEditingSession> findSession(String projectId, Long fileId) {
    Map<Object, Object> payload =
        stringRedisTemplate.opsForHash().entries(fileEditingKey(projectId, fileId));
    if (payload.isEmpty()) {
      return Optional.empty();
    }

    Object userId = payload.get(USER_ID_FIELD);
    Object userName = payload.get(USER_NAME_FIELD);
    Object sessionId = payload.get(SESSION_ID_FIELD);
    Object lastSeenAt = payload.get(LAST_SEEN_AT_FIELD);
    if (userId == null || userName == null || sessionId == null || lastSeenAt == null) {
      throw new IllegalStateException("Failed to read file editing session.");
    }

    return Optional.of(
        new FileEditingSession(
            projectId,
            fileId,
            Long.parseLong(userId.toString()),
            userName.toString(),
            sessionId.toString(),
            Instant.ofEpochMilli(Long.parseLong(lastSeenAt.toString()))));
  }

  @Override
  public void touchSession(FileEditingSession session) {
    String key = fileEditingKey(session.projectId(), session.fileId());
    stringRedisTemplate.opsForHash().putAll(key, writeSession(session));
    stringRedisTemplate.expire(key, SESSION_TTL);
  }

  @Override
  public void removeSession(String projectId, Long fileId, String sessionId) {
    findSession(projectId, fileId)
        .filter(session -> sessionId.equals(session.sessionId()))
        .ifPresent(ignored -> stringRedisTemplate.delete(fileEditingKey(projectId, fileId)));
  }

  private Map<String, String> writeSession(FileEditingSession session) {
    return Map.of(
        USER_ID_FIELD,
        session.userId().toString(),
        USER_NAME_FIELD,
        session.userName(),
        SESSION_ID_FIELD,
        session.sessionId(),
        LAST_SEEN_AT_FIELD,
        String.valueOf(session.lastSeenAt().toEpochMilli()));
  }

  private String fileEditingKey(String projectId, Long fileId) {
    return FILE_EDITING_KEY_PREFIX + projectId + ":" + fileId;
  }
}
