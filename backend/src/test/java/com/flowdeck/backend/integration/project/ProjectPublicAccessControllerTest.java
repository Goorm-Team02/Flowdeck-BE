package com.flowdeck.backend.integration.project;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.domain.ProjectVisibility;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import com.flowdeck.testsupport.AuthenticatedWebIntegrationTest;
import com.flowdeck.testsupport.JwtBearerTokenTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@AuthenticatedWebIntegrationTest
class ProjectPublicAccessControllerTest extends JwtBearerTokenTestSupport {

  private final MockMvc mockMvc;
  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;

  private Project publicProject;
  private User user;

  @Autowired
  ProjectPublicAccessControllerTest(
      MockMvc mockMvc,
      ProjectRepository projectRepository,
      UserRepository userRepository) {
    this.mockMvc = mockMvc;
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
    user = userRepository.save(new User("public-reader@test.com", "password", "reader"));
  }

  @Test
  void publicProjectsRejectAnonymousRequest() throws Exception {
    mockMvc.perform(get("/api/projects/public")).andExpect(status().isUnauthorized());
  }

  @Test
  void authenticatedUserCanReadPublicProjectEndpoints() throws Exception {
    mockMvc
        .perform(get("/api/projects/public").header(AUTHORIZATION, bearerToken(user.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.projects[0].id").value(publicProject.getPublicId()))
        .andExpect(jsonPath("$.data.projects[0].visibility").value("PUBLIC"));

    mockMvc
        .perform(
            get("/api/projects/public/{projectId}", publicProject.getPublicId())
                .header(AUTHORIZATION, bearerToken(user.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(publicProject.getPublicId()))
        .andExpect(jsonPath("$.data.visibility").value("PUBLIC"));
  }
}
