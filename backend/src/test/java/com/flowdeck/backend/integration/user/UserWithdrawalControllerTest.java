package com.flowdeck.backend.integration.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flowdeck.backend.auth.service.AuthTokenService;
import com.flowdeck.backend.global.security.jwt.JwtTokenProvider;
import com.flowdeck.backend.member.domain.ProjectMember;
import com.flowdeck.backend.member.domain.ProjectRole;
import com.flowdeck.backend.member.repository.ProjectMemberRepository;
import com.flowdeck.backend.presence.domain.ProjectPresenceSession;
import com.flowdeck.backend.presence.store.ProjectPresenceStore;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.domain.ProjectVisibility;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import com.flowdeck.testsupport.AuthenticatedWebIntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@AuthenticatedWebIntegrationTest
@Import(UserWithdrawalControllerTest.FakePresenceStoreConfig.class)
class UserWithdrawalControllerTest {

  private final MockMvc mockMvc;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;
  private final AuthTokenService authTokenService;
  private final UserRepository userRepository;
  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final ProjectPresenceStore projectPresenceStore;

  @Autowired
  UserWithdrawalControllerTest(
      MockMvc mockMvc,
      JwtTokenProvider jwtTokenProvider,
      PasswordEncoder passwordEncoder,
      AuthTokenService authTokenService,
      UserRepository userRepository,
      ProjectRepository projectRepository,
      ProjectMemberRepository projectMemberRepository,
      ProjectPresenceStore projectPresenceStore) {
    this.mockMvc = mockMvc;
    this.jwtTokenProvider = jwtTokenProvider;
    this.passwordEncoder = passwordEncoder;
    this.authTokenService = authTokenService;
    this.userRepository = userRepository;
    this.projectRepository = projectRepository;
    this.projectMemberRepository = projectMemberRepository;
    this.projectPresenceStore = projectPresenceStore;
  }

  @BeforeEach
  void setUp() {
    ((FakeProjectPresenceStore) projectPresenceStore).clear();
    projectMemberRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void withdrawMyAccountMasksUserAndCleansTokens() throws Exception {
    User owner =
        userRepository.save(
            new User("withdraw-owner@test.com", passwordEncoder.encode("password123"), "owner"));
    User user =
        userRepository.save(
            new User("withdraw-user@test.com", passwordEncoder.encode("password123"), "user"));
    Project project =
        projectRepository.save(new Project("project", "description", ProjectVisibility.PRIVATE));
    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));
    projectMemberRepository.save(new ProjectMember(project, user, ProjectRole.EDITOR));
    projectPresenceStore.touchSession(
        "withdraw-session-1",
        new ProjectPresenceSession(project.getPublicId(), user.getId(), Instant.now()));
    String accessToken = createAccessToken(user);

    mockMvc
        .perform(delete("/api/users/me").header(AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("회원 탈퇴가 완료되었습니다."));

    User withdrawnUser = userRepository.findById(user.getId()).orElseThrow();
    assertThat(withdrawnUser.getDeletedAt()).isNotNull();
    assertThat(withdrawnUser.getName()).isEqualTo("탈퇴한 사용자");
    assertThat(withdrawnUser.getEmail())
        .startsWith("deleted+" + user.getId() + "+")
        .endsWith("@flowdeck.local");
    assertThat(projectMemberRepository.findByProjectAndUser(project, withdrawnUser)).isEmpty();
    verify(authTokenService).deleteRefreshToken(user.getId());
    verify(authTokenService).blacklistAccessToken(eq(accessToken), any(Duration.class));
    verify(authTokenService).forceLogout(eq(user.getId()), any(Duration.class));
    assertThat(projectPresenceStore.findSession("withdraw-session-1")).isEmpty();
    assertThat(projectPresenceStore.findActiveSessions(project.getPublicId(), Instant.now()))
        .noneMatch(session -> user.getId().equals(session.userId()));
  }

  @Test
  void lastOwnerCannotWithdraw() throws Exception {
    User owner =
        userRepository.save(
            new User("last-owner@test.com", passwordEncoder.encode("password123"), "owner"));
    Project project =
        projectRepository.save(new Project("project", "description", ProjectVisibility.PRIVATE));
    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));
    String accessToken = createAccessToken(owner);

    mockMvc
        .perform(delete("/api/users/me").header(AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("USER_409"));
  }

  @Test
  void withdrawnUserCannotLoginOrRefreshAndEmailCanBeReused() throws Exception {
    String email = "reuse@test.com";
    User user =
        userRepository.save(
            new User(email, passwordEncoder.encode("password123"), "withdraw target"));
    String accessToken = createAccessToken(user);
    String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getPublicId());
    when(authTokenService.matchesRefreshToken(user.getId(), refreshToken)).thenReturn(true);

    mockMvc
        .perform(delete("/api/users/me").header(AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content(loginRequest(email, "password123")))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType("application/json")
                .content(refreshRequest(refreshToken)))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            post("/api/auth/signup")
                .contentType("application/json")
                .content(signupRequest(email, "password123", "new user")))
        .andExpect(status().isOk());
  }

  private String createAccessToken(User user) {
    return jwtTokenProvider.createAccessToken(
        user.getId(), user.getPublicId(), List.of("ROLE_USER"));
  }

  private String loginRequest(String email, String password) {
    return """
        {"email":"%s","password":"%s"}
        """
        .formatted(email, password);
  }

  private String refreshRequest(String refreshToken) {
    return """
        {"refreshToken":"%s"}
        """
        .formatted(refreshToken);
  }

  private String signupRequest(String email, String password, String name) {
    return """
        {"email":"%s","password":"%s","name":"%s"}
        """
        .formatted(email, password, name);
  }

  @TestConfiguration
  static class FakePresenceStoreConfig {

    @Bean
    @Primary
    ProjectPresenceStore projectPresenceStore() {
      return new FakeProjectPresenceStore();
    }
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
    public void removeSessionsByUserId(Long userId) {
      sessions.entrySet().removeIf(entry -> userId.equals(entry.getValue().userId()));
    }

    @Override
    public List<ProjectPresenceSession> findActiveSessions(String projectId, Instant now) {
      return sessions.values().stream()
          .filter(session -> projectId.equals(session.projectId()))
          .toList();
    }

    private void clear() {
      sessions.clear();
    }
  }
}
