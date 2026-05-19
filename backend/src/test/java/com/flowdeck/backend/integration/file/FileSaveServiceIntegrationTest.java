package com.flowdeck.backend.integration.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.flowdeck.backend.file.domain.FileType;
import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.file.repository.ProjectFileRepository;
import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import com.flowdeck.backend.member.domain.ProjectMember;
import com.flowdeck.backend.member.domain.ProjectRole;
import com.flowdeck.backend.member.repository.ProjectMemberRepository;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.domain.ProjectVisibility;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import com.flowdeck.backend.version.domain.FileVersion;
import com.flowdeck.backend.version.dto.FileSaveRequest;
import com.flowdeck.backend.version.dto.FileSaveResponse;
import com.flowdeck.backend.version.repository.FileVersionRepository;
import com.flowdeck.backend.version.service.FileSaveService;
import com.flowdeck.testsupport.DatabaseIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@DatabaseIntegrationTest
@Transactional
class FileSaveServiceIntegrationTest {

  private final ProjectRepository projectRepository;
  private final ProjectFileRepository projectFileRepository;
  private final FileVersionRepository fileVersionRepository;
  private final FileSaveService fileSaveService;
  private final UserRepository userRepository;
  private final ProjectMemberRepository projectMemberRepository;

  @Autowired
  FileSaveServiceIntegrationTest(
      ProjectRepository projectRepository,
      ProjectFileRepository projectFileRepository,
      FileVersionRepository fileVersionRepository,
      FileSaveService fileSaveService,
      UserRepository userRepository,
      ProjectMemberRepository projectMemberRepository) {
    this.projectRepository = projectRepository;
    this.projectFileRepository = projectFileRepository;
    this.fileVersionRepository = fileVersionRepository;
    this.fileSaveService = fileSaveService;
    this.userRepository = userRepository;
    this.projectMemberRepository = projectMemberRepository;
  }

  @Test
  void saveFileUpdatesCurrentContentWithoutCreatingVersion() {
    Project project =
        projectRepository.save(new Project("project", "description", ProjectVisibility.PRIVATE));
    User owner = userRepository.save(new User("save-owner@test.com", "password", "owner"));
    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));

    ProjectFile file =
        projectFileRepository.save(new ProjectFile(project, null, "Main.java", FileType.FILE));

    FileSaveResponse firstResponse =
        fileSaveService.saveFile(
            project.getPublicId(),
            owner.getId(),
            file.getId(),
            createSaveRequest("class Main {}", 0L, "first save"));

    FileSaveResponse secondResponse =
        fileSaveService.saveFile(
            project.getPublicId(),
            owner.getId(),
            file.getId(),
            createSaveRequest("class Main { void run() {} }", 1L, "second save"));

    List<FileVersion> versions = fileVersionRepository.findAllByFileOrderByVersionNumberDesc(file);

    assertThat(firstResponse.currentVersion()).isZero();
    assertThat(firstResponse.editRevision()).isEqualTo(1);
    assertThat(secondResponse.currentVersion()).isZero();
    assertThat(secondResponse.editRevision()).isEqualTo(2);
    assertThat(file.getCurrentVersion()).isZero();
    assertThat(file.getEditRevision()).isEqualTo(2);
    assertThat(file.getCurrentContent()).isEqualTo("class Main { void run() {} }");
    assertThat(versions).isEmpty();
  }

  @Test
  void saveFileFailsWhenBaseRevisionDoesNotMatchCurrentRevision() {
    Project project =
        projectRepository.save(
            new Project("conflict project", "description", ProjectVisibility.PRIVATE));
    User owner = userRepository.save(new User("conflict-owner@test.com", "password", "owner"));
    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));

    ProjectFile file =
        projectFileRepository.save(new ProjectFile(project, null, "Main.java", FileType.FILE));

    fileSaveService.saveFile(
        project.getPublicId(),
        owner.getId(),
        file.getId(),
        createSaveRequest("class Main {}", 0L, "first save"));

    assertThatThrownBy(
            () ->
                fileSaveService.saveFile(
                    project.getPublicId(),
                    owner.getId(),
                    file.getId(),
                    createSaveRequest("stale content", 0L, "stale save")))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void saveFileFailsWhenContentSizeExceedsLimit() {
    Project project =
        projectRepository.save(
            new Project("size limit project", "description", ProjectVisibility.PRIVATE));
    User owner = userRepository.save(new User("size-owner@test.com", "password", "owner"));
    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));

    ProjectFile file =
        projectFileRepository.save(new ProjectFile(project, null, "Large.java", FileType.FILE));

    String oversizedContent = "a".repeat(1024 * 1024 + 1);

    assertThatThrownBy(
            () ->
                fileSaveService.saveFile(
                    project.getPublicId(),
                    owner.getId(),
                    file.getId(),
                    createSaveRequest(oversizedContent, 0L, "oversized save")))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.FILE_SIZE_EXCEEDED);
  }

  @Test
  void viewerCannotSaveFile() {
    Project project =
        projectRepository.save(
            new Project("viewer project", "description", ProjectVisibility.PRIVATE));
    User viewer = userRepository.save(new User("save-viewer@test.com", "password", "viewer"));
    projectMemberRepository.save(new ProjectMember(project, viewer, ProjectRole.VIEWER));

    ProjectFile file =
        projectFileRepository.save(new ProjectFile(project, null, "Main.java", FileType.FILE));

    assertThatThrownBy(
            () ->
                fileSaveService.saveFile(
                    project.getPublicId(),
                    viewer.getId(),
                    file.getId(),
                    createSaveRequest("class Main {}", 0L, "viewer save")))
        .isInstanceOf(BusinessException.class);
  }

  private FileSaveRequest createSaveRequest(
      String content, Long baseRevision, String changeMessage) {
    FileSaveRequest request = new FileSaveRequest();
    ReflectionTestUtils.setField(request, "content", content);
    ReflectionTestUtils.setField(request, "baseRevision", baseRevision);
    ReflectionTestUtils.setField(request, "changeMessage", changeMessage);
    return request;
  }
}
