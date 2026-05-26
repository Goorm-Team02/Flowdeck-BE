package com.flowdeck.backend.presence.store;

import com.flowdeck.backend.presence.domain.ProjectPresenceSession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ProjectPresenceStore {

  void touchSession(String sessionId, ProjectPresenceSession session);

  Optional<ProjectPresenceSession> findSession(String sessionId);

  void removeSession(String sessionId);

  void removeSessionsByUserId(Long userId);

  List<ProjectPresenceSession> findActiveSessions(String projectId, Instant now);
}
