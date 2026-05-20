package com.flowdeck.backend.integration.file;

import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
@Import(FileConflictResponseControllerIntegrationTest.TestAuthConfig.class)
class FileConflictResponseControllerIntegrationTest {

  private final MockMvc mockMvc;
  private final JwtTokenProvider jwtTokenProvider;
  private final ProjectRepository projectRepository;
  private final ProjectFileRepository projectFileRepository;
  private final FileVersionRepository fileVersionRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final UserRepository userRepository;

  @Autowired
  FileConflictResponseControllerIntegrationTest(
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
  void saveFileConflictReturnsFileConflictResponseData() throws Exception {
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
                      "content": "stale content",
                      "baseRevision": 0,
                      "changeMessage": "stale save"
                    }
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("FILE_409"))
        .andExpect(jsonPath("$.data.fileId").value(fixture.file().getId()))
        .andExpect(jsonPath("$.data.baseRevision").value(0))
        .andExpect(jsonPath("$.data.currentRevision").value(1))
        .andExpect(jsonPath("$.data.currentVersion").value(0))
        .andExpect(jsonPath("$.data.latestContent").value("class Main {}"));
  }

  @Test
  void restoreVersionConflictReturnsFileConflictResponseData() throws Exception {
    TestFixture fixture = createFixture();
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
                .content("{\"baseRevision\":0}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("FILE_409"))
        .andExpect(jsonPath("$.data.fileId").value(fixture.file().getId()))
        .andExpect(jsonPath("$.data.baseRevision").value(0))
        .andExpect(jsonPath("$.data.currentRevision").value(1))
        .andExpect(jsonPath("$.data.currentVersion").value(0))
        .andExpect(jsonPath("$.data.latestContent").value("class Main {}"));
  }

  @Test
  void deleteFileConflictReturnsFileConflictResponseData() throws Exception {
    TestFixture fixture = createFixture();

    mockMvc
        .perform(
            delete(
                    "/api/projects/{projectId}/files/{fileId}",
                    fixture.project().getPublicId(),
                    fixture.file().getId())
                .header(AUTHORIZATION, bearerToken(fixture.user().getId()))
                .queryParam("expectedRevision", "0"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("FILE_409"))
        .andExpect(jsonPath("$.data.fileId").value(fixture.file().getId()))
        .andExpect(jsonPath("$.data.baseRevision").value(0))
        .andExpect(jsonPath("$.data.currentRevision").value(1))
        .andExpect(jsonPath("$.data.currentVersion").value(0))
        .andExpect(jsonPath("$.data.latestContent").value("class Main {}"));
  }

  private TestFixture createFixture() {
    Project project =
        projectRepository.save(
            new Project("conflict project", "description", ProjectVisibility.PRIVATE));
    User user = userRepository.save(new User("conflict-api@test.com", "password", "owner"));
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
