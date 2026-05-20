package com.flowdeck.backend.unit.presence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flowdeck.backend.permission.service.PermissionService;
import com.flowdeck.backend.presence.domain.ProjectPresenceSession;
import com.flowdeck.backend.presence.dto.ProjectPresenceMemberResponse;
import com.flowdeck.backend.presence.dto.ProjectPresenceResponse;
import com.flowdeck.backend.presence.service.ProjectPresenceService;
import com.flowdeck.backend.presence.store.ProjectPresenceStore;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ProjectPresenceServiceTest {

  private static final String PROJECT_ID = "project-123";

  private FakeProjectPresenceStore projectPresenceStore;
  private PermissionService permissionService;
  private UserRepository userRepository;
  private ProjectPresenceService projectPresenceService;

  @BeforeEach
  void setUp() {
    projectPresenceStore = new FakeProjectPresenceStore();
    permissionService = org.mockito.Mockito.mock(PermissionService.class);
    userRepository = org.mockito.Mockito.mock(UserRepository.class);
    projectPresenceService =
        new ProjectPresenceService(projectPresenceStore, permissionService, userRepository);
  }

  @Test
  void joinStoresSessionAndReturnsPresenceSnapshot() {
    when(userRepository.findAllById(any())).thenReturn(List.of(user(7L, "owner")));

    ProjectPresenceResponse response = projectPresenceService.join(PROJECT_ID, 7L, "session-1");

    verify(permissionService).validateProjectAccess(PROJECT_ID, 7L);
    assertThat(projectPresenceStore.findSession("session-1"))
        .hasValueSatisfying(
            session -> {
              assertThat(session.projectId()).isEqualTo(PROJECT_ID);
              assertThat(session.userId()).isEqualTo(7L);
            });
    assertThat(response.connectedCount()).isEqualTo(1);
    assertThat(response.members())
        .singleElement()
        .extracting(ProjectPresenceMemberResponse::userName)
        .isEqualTo("owner");
  }

  @Test
  void getPresenceGroupsMultipleSessionsByUser() {
    Instant now = Instant.now();
    projectPresenceStore.touchSession(
        "session-1", new ProjectPresenceSession(PROJECT_ID, 7L, now.minusSeconds(5)));
    projectPresenceStore.touchSession(
        "session-2", new ProjectPresenceSession(PROJECT_ID, 7L, now));
    projectPresenceStore.touchSession(
        "session-3", new ProjectPresenceSession(PROJECT_ID, 8L, now.minusSeconds(2)));
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
  void leaveRemovesSessionAndReturnsUpdatedPresence() {
    Instant now = Instant.now();
    projectPresenceStore.touchSession("session-1", new ProjectPresenceSession(PROJECT_ID, 7L, now));
    projectPresenceStore.touchSession(
        "session-2", new ProjectPresenceSession(PROJECT_ID, 8L, now.minusSeconds(1)));
    when(userRepository.findAllById(any())).thenReturn(List.of(user(8L, "editor")));

    Optional<ProjectPresenceResponse> response = projectPresenceService.leave("session-1");

    assertThat(response).isPresent();
    assertThat(projectPresenceStore.findSession("session-1")).isEmpty();
    assertThat(response.orElseThrow().connectedCount()).isEqualTo(1);
    assertThat(response.orElseThrow().members())
        .singleElement()
        .extracting(ProjectPresenceMemberResponse::userName)
        .isEqualTo("editor");
  }

  private User user(Long id, String name) {
    User user = new User(name + "@test.com", "password", name);
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }

  private static final class FakeProjectPresenceStore implements ProjectPresenceStore {

    private final Map<String, ProjectPresenceSession> sessions = new HashMap<>();

    @Override
    public void touchSession(String sessionId, ProjectPresenceSession session) {
      sessions.put(sessionId, session);
    }

    @Override
    public Optional<ProjectPresenceSession> findSession(String sessionId) {
      return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public void removeSession(String sessionId) {
      sessions.remove(sessionId);
    }

    @Override
    public List<ProjectPresenceSession> findActiveSessions(String projectId, Instant now) {
      return sessions.values().stream()
          .filter(session -> projectId.equals(session.projectId()))
          .toList();
    }
  }
}
