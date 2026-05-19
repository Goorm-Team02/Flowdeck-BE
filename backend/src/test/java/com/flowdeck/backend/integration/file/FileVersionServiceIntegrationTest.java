package com.flowdeck.backend.integration.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.flowdeck.backend.file.domain.FileType;
import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.file.repository.ProjectFileRepository;
import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.member.domain.ProjectMember;
import com.flowdeck.backend.member.domain.ProjectRole;
import com.flowdeck.backend.member.repository.ProjectMemberRepository;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.domain.ProjectVisibility;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import com.flowdeck.backend.version.domain.FileVersion;
import com.flowdeck.backend.version.dto.FileVersionCreateRequest;
import com.flowdeck.backend.version.dto.FileVersionCreateResponse;
import com.flowdeck.backend.version.dto.FileVersionRestoreResponse;
import com.flowdeck.backend.version.repository.FileVersionRepository;
import com.flowdeck.backend.version.service.FileVersionService;
import com.flowdeck.testsupport.DatabaseIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@DatabaseIntegrationTest
@Transactional
class FileVersionServiceIntegrationTest {

  private final ProjectRepository projectRepository;
  private final ProjectFileRepository projectFileRepository;
  private final FileVersionRepository fileVersionRepository;
  private final FileVersionService fileVersionService;
  private final UserRepository userRepository;
  private final ProjectMemberRepository projectMemberRepository;

  @Autowired
  FileVersionServiceIntegrationTest(
      ProjectRepository projectRepository,
      ProjectFileRepository projectFileRepository,
      FileVersionRepository fileVersionRepository,
      FileVersionService fileVersionService,
      UserRepository userRepository,
      ProjectMemberRepository projectMemberRepository) {
    this.projectRepository = projectRepository;
    this.projectFileRepository = projectFileRepository;
    this.fileVersionRepository = fileVersionRepository;
    this.fileVersionService = fileVersionService;
    this.userRepository = userRepository;
    this.projectMemberRepository = projectMemberRepository;
  }

  @Test
  void createVersionSnapshotsCurrentContentAndIncreasesCurrentVersion() {
    Project project =
        projectRepository.save(
            new Project("version create project", "description", ProjectVisibility.PRIVATE));
    User owner =
        userRepository.save(new User("version-create-owner@test.com", "password", "owner"));
    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));

    ProjectFile file =
        projectFileRepository.save(new ProjectFile(project, null, "Main.java", FileType.FILE));
    file.updateContent("class Main { void run() {} }");

    FileVersionCreateResponse response =
        fileVersionService.createVersion(
            project.getPublicId(), owner.getId(), file.getId(), createVersionRequest("first save"));

    List<FileVersion> versions = fileVersionRepository.findAllByFileOrderByVersionNumberDesc(file);

    assertThat(response.currentVersion()).isEqualTo(1);
    assertThat(file.getCurrentVersion()).isEqualTo(1);
    assertThat(versions).hasSize(1);
    assertThat(versions.get(0).getVersionNumber()).isEqualTo(1);
    assertThat(versions.get(0).getContent()).isEqualTo("class Main { void run() {} }");
    assertThat(versions.get(0).getChangeMessage()).isEqualTo("first save");
    assertThat(versions.get(0).getUserId()).isEqualTo(owner.getId());
  }

  @Test
  void restoreVersionCreatesNewVersionWithoutChangingPreviousVersions() {
    Project project =
        projectRepository.save(new Project("project", "description", ProjectVisibility.PRIVATE));
    User owner = userRepository.save(new User("restore-owner@test.com", "password", "owner"));
    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));

    ProjectFile file =
        projectFileRepository.save(new ProjectFile(project, null, "Main.java", FileType.FILE));

    file.increaseVersion();
    FileVersion firstVersion =
        fileVersionRepository.save(new FileVersion(file, null, 1, "v1 content", "first save"));

    file.increaseVersion();
    fileVersionRepository.save(new FileVersion(file, null, 2, "v2 content", "second save"));

    FileVersionRestoreResponse response =
        fileVersionService.restoreVersion(
            project.getPublicId(), owner.getId(), file.getId(), firstVersion.getId());

    List<FileVersion> versions = fileVersionRepository.findAllByFileOrderByVersionNumberDesc(file);

    assertThat(response.currentVersion()).isEqualTo(3);
    assertThat(file.getCurrentVersion()).isEqualTo(3);
    assertThat(file.getCurrentContent()).isEqualTo("v1 content");
    assertThat(versions).hasSize(3);
    assertThat(versions).extracting(FileVersion::getVersionNumber).containsExactly(3, 2, 1);
    assertThat(versions.get(0).getUserId()).isEqualTo(owner.getId());
    assertThat(versions.get(0).getContent()).isEqualTo("v1 content");
    assertThat(versions.get(1).getContent()).isEqualTo("v2 content");
    assertThat(versions.get(2).getContent()).isEqualTo("v1 content");
  }

  @Test
  void viewerCannotRestoreVersion() {
    Project project =
        projectRepository.save(
            new Project("viewer restore project", "description", ProjectVisibility.PRIVATE));
    User viewer = userRepository.save(new User("restore-viewer@test.com", "password", "viewer"));
    projectMemberRepository.save(new ProjectMember(project, viewer, ProjectRole.VIEWER));

    ProjectFile file =
        projectFileRepository.save(new ProjectFile(project, null, "Main.java", FileType.FILE));

    file.increaseVersion();
    FileVersion version =
        fileVersionRepository.save(
            new FileVersion(file, viewer.getId(), 1, "v1 content", "first save"));

    assertThatThrownBy(
            () ->
                fileVersionService.restoreVersion(
                    project.getPublicId(), viewer.getId(), file.getId(), version.getId()))
        .isInstanceOf(BusinessException.class);
  }

  private FileVersionCreateRequest createVersionRequest(String changeMessage) {
    FileVersionCreateRequest request = new FileVersionCreateRequest();
    ReflectionTestUtils.setField(request, "changeMessage", changeMessage);
    return request;
  }
}
