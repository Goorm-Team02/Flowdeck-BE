package com.flowdeck.backend.integration.project;

import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flowdeck.backend.auth.service.AuthTokenService;
import com.flowdeck.backend.global.security.jwt.JwtTokenProvider;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.domain.ProjectVisibility;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import com.flowdeck.testsupport.DatabaseIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

@DatabaseIntegrationTest
@AutoConfigureMockMvc
@Import(ProjectPublicAccessControllerIntegrationTest.TestAuthConfig.class)
class ProjectPublicAccessControllerIntegrationTest {

  private final MockMvc mockMvc;
  private final JwtTokenProvider jwtTokenProvider;
  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;

  private Project publicProject;
  private Project privateProject;
  private User user;

  @Autowired
  ProjectPublicAccessControllerIntegrationTest(
      MockMvc mockMvc,
      JwtTokenProvider jwtTokenProvider,
      ProjectRepository projectRepository,
      UserRepository userRepository) {
    this.mockMvc = mockMvc;
    this.jwtTokenProvider = jwtTokenProvider;
    this.projectRepository = projectRepository;
    this.userRepository = userRepository;
  }

  @BeforeEach
  void setUp() {
    projectRepository.deleteAll();
    userRepository.deleteAll();

    publicProject =
        projectRepository.save(
            new Project("public project", "description", ProjectVisibility.PUBLIC));
    privateProject =
        projectRepository.save(
            new Project("private project", "description", ProjectVisibility.PRIVATE));
    user = userRepository.save(new User("public-reader@test.com", "password", "reader"));
  }

  @Test
  void publicProjectsRejectAnonymousRequest() throws Exception {
    mockMvc.perform(get("/api/projects/public")).andExpect(status().isUnauthorized());
  }

  @Test
  void publicProjectsAllowAuthenticatedUser() throws Exception {
    mockMvc
        .perform(get("/api/projects/public").header(AUTHORIZATION, bearerToken(user.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.projects[0].id").value(publicProject.getPublicId()))
        .andExpect(jsonPath("$.data.projects[0].visibility").value("PUBLIC"));
  }

  @Test
  void publicProjectDetailAllowsAuthenticatedNonMember() throws Exception {
    mockMvc
        .perform(
            get("/api/projects/public/{projectId}", publicProject.getPublicId())
                .header(AUTHORIZATION, bearerToken(user.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(publicProject.getPublicId()))
        .andExpect(jsonPath("$.data.visibility").value("PUBLIC"));
  }

  @Test
  void privateProjectDetailRejectsPublicEndpoint() throws Exception {
    mockMvc
        .perform(
            get("/api/projects/public/{projectId}", privateProject.getPublicId())
                .header(AUTHORIZATION, bearerToken(user.getId())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_403"));
  }

  private String bearerToken(Long userId) {
    return "Bearer "
        + jwtTokenProvider.createAccessToken(
            userId, "tester" + userId + "@flowdeck.com", List.of("ROLE_USER"));
  }

  @TestConfiguration
  static class TestAuthConfig {

    @Bean
    @Primary
    AuthTokenService authTokenService() {
      return mock(AuthTokenService.class);
    }
  }
}
