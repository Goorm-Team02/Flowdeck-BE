package com.flowdeck.backend.unit.fileediting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flowdeck.backend.file.domain.FileType;
import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.file.repository.ProjectFileRepository;
import com.flowdeck.backend.fileediting.domain.FileEditingSession;
import com.flowdeck.backend.fileediting.dto.FileEditingResponse;
import com.flowdeck.backend.fileediting.service.FileEditingService;
import com.flowdeck.backend.fileediting.store.FileEditingStore;
import com.flowdeck.backend.permission.service.PermissionService;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.domain.ProjectVisibility;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class FileEditingServiceTest {

  private static final String PROJECT_ID = "project-123";
  private static final Long FILE_ID = 11L;

  private InMemoryFileEditingStore fileEditingStore;
  private PermissionService permissionService;
  private ProjectRepository projectRepository;
  private ProjectFileRepository projectFileRepository;
  private UserRepository userRepository;
  private FileEditingService fileEditingService;

  @BeforeEach
  void setUp() {
    fileEditingStore = new InMemoryFileEditingStore();
    permissionService = org.mockito.Mockito.mock(PermissionService.class);
    projectRepository = org.mockito.Mockito.mock(ProjectRepository.class);
    projectFileRepository = org.mockito.Mockito.mock(ProjectFileRepository.class);
    userRepository = org.mockito.Mockito.mock(UserRepository.class);
    fileEditingService =
        new FileEditingService(
            fileEditingStore,
            permissionService,
            projectRepository,
            projectFileRepository,
            userRepository);

    Project project = new Project("project", "description", ProjectVisibility.PRIVATE);
    ProjectFile file = new ProjectFile(project, null, "Main.java", FileType.FILE);
    ReflectionTestUtils.setField(file, "id", FILE_ID);
    when(projectRepository.findByPublicId(PROJECT_ID)).thenReturn(Optional.of(project));
    when(projectFileRepository.findByIdAndProject(FILE_ID, project)).thenReturn(Optional.of(file));
  }

  @Test
  void startEditingClaimsFileWhenNoEditorExists() {
    User editor = user(7L, "editor");
    when(userRepository.findById(7L)).thenReturn(Optional.of(editor));

    FileEditingResponse response =
        fileEditingService.startEditing(PROJECT_ID, FILE_ID, 7L, "session-1");

    verify(permissionService).validateEditor(PROJECT_ID, 7L);
    assertThat(response.editing()).isTrue();
    assertThat(response.editorId()).isEqualTo(7L);
    assertThat(response.editorName()).isEqualTo("editor");
    assertThat(response.editorSessionId()).isEqualTo("session-1");
    assertThat(fileEditingStore.session).isNotNull();
  }

  @Test
  void startEditingReturnsCurrentEditorWhenAnotherUserIsEditing() {
    fileEditingStore.session =
        new FileEditingSession(
            PROJECT_ID, FILE_ID, 7L, "editor", "session-1", Instant.parse("2026-05-20T12:00:00Z"));

    FileEditingResponse response =
        fileEditingService.startEditing(PROJECT_ID, FILE_ID, 8L, "session-2");

    verify(permissionService).validateEditor(PROJECT_ID, 8L);
    assertThat(response.editing()).isTrue();
    assertThat(response.editorId()).isEqualTo(7L);
    assertThat(response.editorName()).isEqualTo("editor");
    assertThat(response.editorSessionId()).isEqualTo("session-1");
  }

  @Test
  void stopEditingClearsOnlyMatchingSession() {
    fileEditingStore.session =
        new FileEditingSession(
            PROJECT_ID, FILE_ID, 7L, "editor", "session-1", Instant.parse("2026-05-20T12:00:00Z"));

    FileEditingResponse response =
        fileEditingService.stopEditing(PROJECT_ID, FILE_ID, 7L, "session-1");

    assertThat(response.editing()).isFalse();
    assertThat(fileEditingStore.session).isNull();
  }

  private User user(Long id, String name) {
    User user = new User(name + "@test.com", "password", name);
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }

  private static class InMemoryFileEditingStore implements FileEditingStore {

    private FileEditingSession session;

    @Override
    public Optional<FileEditingSession> findSession(String projectId, Long fileId) {
      if (session == null
          || !projectId.equals(session.projectId())
          || !fileId.equals(session.fileId())) {
        return Optional.empty();
      }
      return Optional.of(session);
    }

    @Override
    public void touchSession(FileEditingSession session) {
      this.session = session;
    }

    @Override
    public void removeSession(String projectId, Long fileId, String sessionId) {
      if (session != null
          && projectId.equals(session.projectId())
          && fileId.equals(session.fileId())
          && sessionId.equals(session.sessionId())) {
        session = null;
      }
    }
  }
}
