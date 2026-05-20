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
@Import(FileErrorResponseControllerIntegrationTest.TestAuthConfig.class)
class FileErrorResponseControllerIntegrationTest {

  private final MockMvc mockMvc;
  private final JwtTokenProvider jwtTokenProvider;
  private final ProjectRepository projectRepository;
  private final ProjectFileRepository projectFileRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final UserRepository userRepository;

  @Autowired
  FileErrorResponseControllerIntegrationTest(
      MockMvc mockMvc,
      JwtTokenProvider jwtTokenProvider,
      ProjectRepository projectRepository,
      ProjectFileRepository projectFileRepository,
      ProjectMemberRepository projectMemberRepository,
      UserRepository userRepository) {
    this.mockMvc = mockMvc;
    this.jwtTokenProvider = jwtTokenProvider;
    this.projectRepository = projectRepository;
    this.projectFileRepository = projectFileRepository;
    this.projectMemberRepository = projectMemberRepository;
    this.userRepository = userRepository;
  }

  @BeforeEach
  void setUp() {
    projectFileRepository.deleteAll();
    projectMemberRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void createFileWithDuplicateNameReturnsFileNameDuplicated() throws Exception {
    TestFixture fixture = createFixture();
    projectFileRepository.save(
        new ProjectFile(fixture.project(), null, "Main.java", FileType.FILE));

    mockMvc
        .perform(
            post("/api/projects/{projectId}/files", fixture.project().getPublicId())
                .header(AUTHORIZATION, bearerToken(fixture.user().getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Main.java",
                      "type": "FILE"
                    }
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("FILE_409_1"))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void saveFolderReturnsFileInvalidType() throws Exception {
    TestFixture fixture = createFixture();
    ProjectFile folder =
        projectFileRepository.save(
            new ProjectFile(fixture.project(), null, "src", FileType.FOLDER));

    mockMvc
        .perform(
            put(
                    "/api/projects/{projectId}/files/{fileId}",
                    fixture.project().getPublicId(),
                    folder.getId())
                .header(AUTHORIZATION, bearerToken(fixture.user().getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "content": "class Main {}",
                      "baseRevision": 0
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("FILE_400"))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void saveOversizedFileReturnsFileSizeExceeded() throws Exception {
    TestFixture fixture = createFixture();
    ProjectFile file =
        projectFileRepository.save(
            new ProjectFile(fixture.project(), null, "Large.java", FileType.FILE));
    String oversizedContent = "a".repeat(1024 * 1024 + 1);

    mockMvc
        .perform(
            put(
                    "/api/projects/{projectId}/files/{fileId}",
                    fixture.project().getPublicId(),
                    file.getId())
                .header(AUTHORIZATION, bearerToken(fixture.user().getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "content": "%s",
                      "baseRevision": 0
                    }
                    """
                        .formatted(oversizedContent)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("FILE_400_3"))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void getMissingVersionReturnsVersionNotFound() throws Exception {
    TestFixture fixture = createFixture();
    ProjectFile file =
        projectFileRepository.save(
            new ProjectFile(fixture.project(), null, "Main.java", FileType.FILE));

    mockMvc
        .perform(
            get(
                    "/api/projects/{projectId}/files/{fileId}/versions/{versionId}",
                    fixture.project().getPublicId(),
                    file.getId(),
                    999L)
                .header(AUTHORIZATION, bearerToken(fixture.user().getId())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("VERSION_404"))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  private TestFixture createFixture() {
    Project project =
        projectRepository.save(
            new Project("error contract project", "description", ProjectVisibility.PRIVATE));
    User user = userRepository.save(new User("file-error@test.com", "password", "owner"));
    projectMemberRepository.save(new ProjectMember(project, user, ProjectRole.OWNER));

    return new TestFixture(project, user);
  }

  private String bearerToken(Long userId) {
    return "Bearer "
        + jwtTokenProvider.createAccessToken(
            userId, "tester" + userId + "@flowdeck.com", List.of("ROLE_USER"));
  }

  private record TestFixture(Project project, User user) {}

  @TestConfiguration
  static class TestAuthConfig {

    @Bean
    @Primary
    AuthTokenService authTokenService() {
      return mock(AuthTokenService.class);
    }
  }
}
