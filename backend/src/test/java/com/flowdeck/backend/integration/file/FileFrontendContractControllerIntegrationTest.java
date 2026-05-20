package com.flowdeck.backend.integration.file;

import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flowdeck.backend.auth.service.AuthTokenService;
import com.flowdeck.backend.file.domain.FileType;
import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.file.repository.ProjectFileRepository;
import com.flowdeck.backend.global.security.jwt.JwtTokenProvider;
import com.flowdeck.backend.member.domain.ProjectMember;
import com.flowdeck.backend.member.domain.ProjectRole;
import com.flowdeck.backend.member.repository.ProjectMemberRepository;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.domain.ProjectVisibility;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import com.flowdeck.backend.version.domain.FileVersion;
import com.flowdeck.backend.version.repository.FileVersionRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@DatabaseIntegrationTest
@AutoConfigureMockMvc
@Import(FileFrontendContractControllerIntegrationTest.TestAuthConfig.class)
class FileFrontendContractControllerIntegrationTest {

  private final MockMvc mockMvc;
  private final JwtTokenProvider jwtTokenProvider;
  private final ProjectRepository projectRepository;
  private final ProjectFileRepository projectFileRepository;
  private final FileVersionRepository fileVersionRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final UserRepository userRepository;

  @Autowired
  FileFrontendContractControllerIntegrationTest(
      MockMvc mockMvc,
      JwtTokenProvider jwtTokenProvider,
      ProjectRepository projectRepository,
      ProjectFileRepository projectFileRepository,
      FileVersionRepository fileVersionRepository,
      ProjectMemberRepository projectMemberRepository,
      UserRepository userRepository) {
    this.mockMvc = mockMvc;
    this.jwtTokenProvider = jwtTokenProvider;
    this.projectRepository = projectRepository;
    this.projectFileRepository = projectFileRepository;
    this.fileVersionRepository = fileVersionRepository;
    this.projectMemberRepository = projectMemberRepository;
    this.userRepository = userRepository;
  }

  @BeforeEach
  void setUp() {
    fileVersionRepository.deleteAll();
    projectFileRepository.deleteAll();
    projectMemberRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void getFileReturnsEditorInitialStateFields() throws Exception {
    TestFixture fixture = createFixture();

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectId}/files/{fileId}",
                    fixture.project().getPublicId(),
                    fixture.file().getId())
                .header(AUTHORIZATION, bearerToken(fixture.user().getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.fileId").value(fixture.file().getId()))
        .andExpect(jsonPath("$.data.parentId").doesNotExist())
        .andExpect(jsonPath("$.data.name").value("Main.java"))
        .andExpect(jsonPath("$.data.type").value("FILE"))
        .andExpect(jsonPath("$.data.currentVersion").value(0))
        .andExpect(jsonPath("$.data.editRevision").value(1))
        .andExpect(jsonPath("$.data.content").value("class Main {}"))
        .andExpect(jsonPath("$.data.createdAt").exists())
        .andExpect(jsonPath("$.data.updatedAt").exists());
  }

  @Test
  void saveFileReturnsStateForFrontendRefresh() throws Exception {
    TestFixture fixture = createFixture();

    mockMvc
        .perform(
            put(
                    "/api/projects/{projectId}/files/{fileId}",
                    fixture.project().getPublicId(),
                    fixture.file().getId())
                .header(AUTHORIZATION, bearerToken(fixture.user().getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "content": "class Main { void run() {} }",
                      "baseRevision": 1,
                      "changeMessage": "save current content"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.fileId").value(fixture.file().getId()))
        .andExpect(jsonPath("$.data.name").value("Main.java"))
        .andExpect(jsonPath("$.data.currentVersion").value(0))
        .andExpect(jsonPath("$.data.editRevision").value(2))
        .andExpect(jsonPath("$.data.updatedAt").exists());
  }

  @Test
  void createVersionReturnsStateForFrontendRefresh() throws Exception {
    TestFixture fixture = createFixture();

    mockMvc
        .perform(
            post(
                    "/api/projects/{projectId}/files/{fileId}/versions",
                    fixture.project().getPublicId(),
                    fixture.file().getId())
                .header(AUTHORIZATION, bearerToken(fixture.user().getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"changeMessage\":\"first version\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.fileId").value(fixture.file().getId()))
        .andExpect(jsonPath("$.data.name").value("Main.java"))
        .andExpect(jsonPath("$.data.currentVersion").value(1))
        .andExpect(jsonPath("$.data.editRevision").value(1))
        .andExpect(jsonPath("$.data.updatedAt").exists());
  }

  @Test
  void restoreVersionReturnsStateForFrontendRefresh() throws Exception {
    TestFixture fixture = createFixture();
    fixture.file().increaseVersion();
    projectFileRepository.save(fixture.file());

    FileVersion version =
        fileVersionRepository.save(
            new FileVersion(fixture.file(), fixture.user().getId(), 1, "class Restored {}", "v1"));

    mockMvc
        .perform(
            post(
                    "/api/projects/{projectId}/files/{fileId}/versions/{versionId}/restore",
                    fixture.project().getPublicId(),
                    fixture.file().getId(),
                    version.getId())
                .header(AUTHORIZATION, bearerToken(fixture.user().getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"baseRevision\":1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.fileId").value(fixture.file().getId()))
        .andExpect(jsonPath("$.data.name").value("Main.java"))
        .andExpect(jsonPath("$.data.currentVersion").value(2))
        .andExpect(jsonPath("$.data.editRevision").value(2))
        .andExpect(jsonPath("$.data.updatedAt").exists());
  }

  private TestFixture createFixture() {
    Project project =
        projectRepository.save(
            new Project("frontend project", "description", ProjectVisibility.PRIVATE));
    User user = userRepository.save(new User("frontend-contract@test.com", "password", "owner"));
    projectMemberRepository.save(new ProjectMember(project, user, ProjectRole.OWNER));

    ProjectFile file = new ProjectFile(project, null, "Main.java", FileType.FILE);
    file.updateContent("class Main {}");
    file.increaseEditRevision();

    return new TestFixture(project, user, projectFileRepository.save(file));
  }

  private String bearerToken(Long userId) {
    return "Bearer "
        + jwtTokenProvider.createAccessToken(
            userId, "tester" + userId + "@flowdeck.com", List.of("ROLE_USER"));
  }

  private record TestFixture(Project project, User user, ProjectFile file) {}

  @TestConfiguration
  static class TestAuthConfig {

    @Bean
    @Primary
    AuthTokenService authTokenService() {
      return mock(AuthTokenService.class);
    }
  }
}
