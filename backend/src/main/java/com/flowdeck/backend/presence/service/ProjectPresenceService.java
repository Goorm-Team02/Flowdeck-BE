package com.flowdeck.backend.presence.service;

import com.flowdeck.backend.permission.service.PermissionService;
import com.flowdeck.backend.presence.domain.ProjectPresenceSession;
import com.flowdeck.backend.presence.dto.ProjectPresenceMemberResponse;
import com.flowdeck.backend.presence.dto.ProjectPresenceResponse;
import com.flowdeck.backend.presence.store.ProjectPresenceStore;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ProjectPresenceService {

  private static final String UNKNOWN_USER_NAME = "알 수 없는 사용자";

  private final ProjectPresenceStore projectPresenceStore;
  private final PermissionService permissionService;
  private final UserRepository userRepository;

  public ProjectPresenceService(
      ProjectPresenceStore projectPresenceStore,
      PermissionService permissionService,
      UserRepository userRepository) {
    this.projectPresenceStore = projectPresenceStore;
    this.permissionService = permissionService;
    this.userRepository = userRepository;
  }

  public ProjectPresenceResponse getPresence(String projectId, Long userId) {
    permissionService.validateProjectAccess(projectId, userId);
    return snapshot(projectId);
  }

  public ProjectPresenceResponse join(String projectId, Long userId, String sessionId) {
    permissionService.validateProjectAccess(projectId, userId);
    projectPresenceStore.touchSession(
        sessionId, new ProjectPresenceSession(projectId, userId, Instant.now()));
    return snapshot(projectId);
  }

  public ProjectPresenceResponse heartbeat(String projectId, Long userId, String sessionId) {
    permissionService.validateProjectAccess(projectId, userId);
    projectPresenceStore.touchSession(
        sessionId, new ProjectPresenceSession(projectId, userId, Instant.now()));
    return snapshot(projectId);
  }

  public Optional<ProjectPresenceResponse> leave(String sessionId) {
    Optional<ProjectPresenceSession> session = projectPresenceStore.findSession(sessionId);
    if (session.isEmpty()) {
      return Optional.empty();
    }

    projectPresenceStore.removeSession(sessionId);
    return Optional.of(snapshot(session.orElseThrow().projectId()));
  }

  private ProjectPresenceResponse snapshot(String projectId) {
    Instant now = Instant.now();
    List<ProjectPresenceSession> sessions = projectPresenceStore.findActiveSessions(projectId, now);
    if (sessions.isEmpty()) {
      return ProjectPresenceResponse.empty(projectId, now);
    }

    Map<Long, Integer> sessionCounts = new LinkedHashMap<>();
    Map<Long, Instant> lastSeenAtByUser = new LinkedHashMap<>();

    for (ProjectPresenceSession session : sessions) {
      sessionCounts.merge(session.userId(), 1, Integer::sum);
      lastSeenAtByUser.compute(
          session.userId(),
          (ignored, previousLastSeenAt) ->
              previousLastSeenAt == null || session.lastSeenAt().isAfter(previousLastSeenAt)
                  ? session.lastSeenAt()
                  : previousLastSeenAt);
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

  private Map<Long, String> userNames(Set<Long> userIds) {
    return userRepository.findAllById(userIds).stream()
        .collect(
            java.util.stream.Collectors.toMap(User::getId, User::getName, (left, right) -> left));
  }
}
