package com.flowdeck.backend.integration.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flowdeck.backend.auth.service.AuthTokenService;
import com.flowdeck.backend.file.domain.FileType;
import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.file.dto.ProjectFileEventResponse;
import com.flowdeck.backend.file.dto.ProjectFileEventType;
import com.flowdeck.backend.file.realtime.ProjectFileBroadcaster;
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
import java.util.ArrayList;
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
@Import(ProjectFileRealtimeControllerIntegrationTest.TestPublisherConfig.class)
class ProjectFileRealtimeControllerIntegrationTest {

  private final MockMvc mockMvc;
  private final JwtTokenProvider jwtTokenProvider;
  private final ProjectRepository projectRepository;
  private final ProjectFileRepository projectFileRepository;
  private final FileVersionRepository fileVersionRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final UserRepository userRepository;
  private final TestProjectFileBroadcaster projectFileBroadcaster;

  @Autowired
  ProjectFileRealtimeControllerIntegrationTest(
      MockMvc mockMvc,
      JwtTokenProvider jwtTokenProvider,
      ProjectRepository projectRepository,
      ProjectFileRepository projectFileRepository,
      FileVersionRepository fileVersionRepository,
      ProjectMemberRepository projectMemberRepository,
      UserRepository userRepository,
      TestProjectFileBroadcaster projectFileBroadcaster) {
    this.mockMvc = mockMvc;
    this.jwtTokenProvider = jwtTokenProvider;
    this.projectRepository = projectRepository;
    this.projectFileRepository = projectFileRepository;
    this.fileVersionRepository = fileVersionRepository;
    this.projectMemberRepository = projectMemberRepository;
    this.userRepository = userRepository;
    this.projectFileBroadcaster = projectFileBroadcaster;
  }

  @BeforeEach
  void setUp() {
    fileVersionRepository.deleteAll();
    projectFileRepository.deleteAll();
    projectMemberRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();
    projectFileBroadcaster.reset();
  }

  @Test
  void createFilePublishesCreatedEvent() throws Exception {
    Project project =
        projectRepository.save(new Project("project", "description", ProjectVisibility.PRIVATE));
    User owner = userRepository.save(new User("create-owner@test.com", "password", "owner"));
    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));
    ProjectFile parentFolder =
        projectFileRepository.save(new ProjectFile(project, null, "src", FileType.FOLDER));

    mockMvc
        .perform(
            post("/api/projects/{projectId}/files", project.getPublicId())
                .header(AUTHORIZATION, bearerToken(owner.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "parentId": %d,
                      "name": "Main.java",
                      "type": "FILE"
                    }
                    """
                        .formatted(parentFolder.getId())))
        .andExpect(status().isOk());

    assertThat(projectFileBroadcaster.events()).hasSize(1);
    ProjectFileEventResponse event = projectFileBroadcaster.events().getFirst();
    assertThat(event.eventType()).isEqualTo(ProjectFileEventType.FILE_CREATED);
    assertThat(event.projectId()).isEqualTo(project.getPublicId());
    assertThat(event.actorId()).isEqualTo(owner.getId());
    assertThat(event.actorName()).isEqualTo(owner.getName());
    assertThat(event.editRevision()).isZero();
    assertThat(event.currentVersion()).isZero();
    assertThat(event.newName()).isEqualTo("Main.java");
    assertThat(event.oldName()).isNull();
    assertThat(event.oldParentId()).isNull();
    assertThat(event.newParentId()).isEqualTo(parentFolder.getId());
    assertThat(event.deletedFileIds()).isNull();
    assertThat(event.occurredAt()).isNotNull();
  }

  @Test
  void saveFilePublishesSavedEvent() throws Exception {
    FileFixture fixture = createFileFixture("save-owner@test.com", "owner", "Main.java");

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
                      "content": "class Main {}",
                      "baseRevision": 0,
                      "changeMessage": "save"
                    }
                    """))
        .andExpect(status().isOk());

    assertThat(projectFileBroadcaster.events()).hasSize(1);
    ProjectFileEventResponse event = projectFileBroadcaster.events().getFirst();
    assertThat(event.eventType()).isEqualTo(ProjectFileEventType.FILE_SAVED);
    assertThat(event.projectId()).isEqualTo(fixture.project().getPublicId());
    assertThat(event.fileId()).isEqualTo(fixture.file().getId());
    assertThat(event.actorId()).isEqualTo(fixture.user().getId());
    assertThat(event.actorName()).isEqualTo(fixture.user().getName());
    assertThat(event.editRevision()).isEqualTo(1);
    assertThat(event.currentVersion()).isZero();
    assertThat(event.occurredAt()).isNotNull();
    assertThat(event.deletedFileIds()).isNull();
  }

  @Test
  void restoreVersionPublishesRestoredEvent() throws Exception {
    FileFixture fixture = createFileFixture("restore-owner@test.com", "owner", "Main.java");
    fixture.file().increaseVersion();
    fixture.file().updateContent("class Current {}");
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
                .content("{\"baseRevision\":0}"))
        .andExpect(status().isOk());

    assertThat(projectFileBroadcaster.events()).hasSize(1);
    ProjectFileEventResponse event = projectFileBroadcaster.events().getFirst();
    assertThat(event.eventType()).isEqualTo(ProjectFileEventType.FILE_RESTORED);
    assertThat(event.projectId()).isEqualTo(fixture.project().getPublicId());
    assertThat(event.fileId()).isEqualTo(fixture.file().getId());
    assertThat(event.actorId()).isEqualTo(fixture.user().getId());
    assertThat(event.actorName()).isEqualTo(fixture.user().getName());
    assertThat(event.editRevision()).isEqualTo(1);
    assertThat(event.currentVersion()).isEqualTo(2);
  }

  @Test
  void renameFilePublishesRenamedEvent() throws Exception {
    FileFixture fixture = createFileFixture("rename-owner@test.com", "owner", "Main.java");

    mockMvc
        .perform(
            patch(
                    "/api/projects/{projectId}/files/{fileId}",
                    fixture.project().getPublicId(),
                    fixture.file().getId())
                .header(AUTHORIZATION, bearerToken(fixture.user().getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"App.java\"}"))
        .andExpect(status().isOk());

    assertThat(projectFileBroadcaster.events()).hasSize(1);
    ProjectFileEventResponse event = projectFileBroadcaster.events().getFirst();
    assertThat(event.eventType()).isEqualTo(ProjectFileEventType.FILE_RENAMED);
    assertThat(event.oldName()).isEqualTo("Main.java");
    assertThat(event.newName()).isEqualTo("App.java");
    assertThat(event.oldParentId()).isNull();
    assertThat(event.newParentId()).isNull();
  }

  @Test
  void moveFilePublishesMovedEvent() throws Exception {
    FileFixture fixture = createFileFixture("move-owner@test.com", "owner", "Main.java");
    ProjectFile targetFolder =
        projectFileRepository.save(
            new ProjectFile(fixture.project(), null, "src", FileType.FOLDER));

    mockMvc
        .perform(
            patch(
                    "/api/projects/{projectId}/files/{fileId}/move",
                    fixture.project().getPublicId(),
                    fixture.file().getId())
                .header(AUTHORIZATION, bearerToken(fixture.user().getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(("{\"parentId\":%d}").formatted(targetFolder.getId())))
        .andExpect(status().isOk());

    assertThat(projectFileBroadcaster.events()).hasSize(1);
    ProjectFileEventResponse event = projectFileBroadcaster.events().getFirst();
    assertThat(event.eventType()).isEqualTo(ProjectFileEventType.FILE_MOVED);
    assertThat(event.oldParentId()).isNull();
    assertThat(event.newParentId()).isEqualTo(targetFolder.getId());
    assertThat(event.oldName()).isNull();
    assertThat(event.newName()).isNull();
  }

  @Test
  void deleteFolderPublishesDeletedEventWithDescendantIds() throws Exception {
    Project project =
        projectRepository.save(
            new Project("delete project", "description", ProjectVisibility.PRIVATE));
    User owner = userRepository.save(new User("delete-owner@test.com", "password", "owner"));
    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));

    ProjectFile folder =
        projectFileRepository.save(new ProjectFile(project, null, "src", FileType.FOLDER));
    ProjectFile childFile =
        projectFileRepository.save(new ProjectFile(project, folder, "Main.java", FileType.FILE));

    mockMvc
        .perform(
            delete(
                    "/api/projects/{projectId}/files/{fileId}",
                    project.getPublicId(),
                    folder.getId())
                .header(AUTHORIZATION, bearerToken(owner.getId()))
                .queryParam("expectedRevision", "0"))
        .andExpect(status().isOk());

    assertThat(projectFileBroadcaster.events()).hasSize(1);
    ProjectFileEventResponse event = projectFileBroadcaster.events().getFirst();
    assertThat(event.eventType()).isEqualTo(ProjectFileEventType.FILE_DELETED);
    assertThat(event.fileId()).isEqualTo(folder.getId());
    assertThat(event.actorId()).isEqualTo(owner.getId());
    assertThat(event.actorName()).isEqualTo(owner.getName());
    assertThat(event.deletedFileIds()).containsExactly(folder.getId(), childFile.getId());
  }

  private FileFixture createFileFixture(String email, String name, String fileName) {
    Project project =
        projectRepository.save(new Project("project", "description", ProjectVisibility.PRIVATE));
    User user = userRepository.save(new User(email, "password", name));
    projectMemberRepository.save(new ProjectMember(project, user, ProjectRole.OWNER));
    ProjectFile file =
        projectFileRepository.save(new ProjectFile(project, null, fileName, FileType.FILE));
    return new FileFixture(project, user, file);
  }

  private String bearerToken(Long userId) {
    return "Bearer "
        + jwtTokenProvider.createAccessToken(
            userId, "tester" + userId + "@flowdeck.com", List.of("ROLE_USER"));
  }

  private record FileFixture(Project project, User user, ProjectFile file) {}

  @TestConfiguration
  static class TestPublisherConfig {

    @Bean
    @Primary
    TestProjectFileBroadcaster projectFileBroadcaster() {
      return new TestProjectFileBroadcaster();
    }

    @Bean
    @Primary
    AuthTokenService authTokenService() {
      return mock(AuthTokenService.class);
    }
  }

  static class TestProjectFileBroadcaster implements ProjectFileBroadcaster {

    private final List<ProjectFileEventResponse> events = new ArrayList<>();

    @Override
    public void broadcast(ProjectFileEventResponse event) {
      events.add(event);
    }

    List<ProjectFileEventResponse> events() {
      return events;
    }

    void reset() {
      events.clear();
    }
  }
}
