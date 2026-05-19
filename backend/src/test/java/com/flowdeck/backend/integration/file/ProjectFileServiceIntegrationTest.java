package com.flowdeck.backend.integration.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.flowdeck.backend.file.domain.FileType;
import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.file.repository.ProjectFileRepository;
import com.flowdeck.backend.file.service.ProjectFileService;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DatabaseIntegrationTest
class ProjectFileServiceIntegrationTest {

  private final ProjectRepository projectRepository;
  private final ProjectFileRepository projectFileRepository;
  private final FileVersionRepository fileVersionRepository;
  private final ProjectFileService projectFileService;
  private final UserRepository userRepository;
  private final ProjectMemberRepository projectMemberRepository;

  @Autowired
  ProjectFileServiceIntegrationTest(
      ProjectRepository projectRepository,
      ProjectFileRepository projectFileRepository,
      FileVersionRepository fileVersionRepository,
      ProjectFileService projectFileService,
      UserRepository userRepository,
      ProjectMemberRepository projectMemberRepository) {
    this.projectRepository = projectRepository;
    this.projectFileRepository = projectFileRepository;
    this.fileVersionRepository = fileVersionRepository;
    this.projectFileService = projectFileService;
    this.userRepository = userRepository;
    this.projectMemberRepository = projectMemberRepository;
  }

  @Test
  void deleteFileAlsoDeletesFileVersions() {
    Project project =
        projectRepository.save(new Project("project", "description", ProjectVisibility.PRIVATE));
    User owner = userRepository.save(new User("delete-file-owner@test.com", "password", "owner"));
    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));
    ProjectFile file =
        projectFileRepository.save(new ProjectFile(project, null, "Main.java", FileType.FILE));
    fileVersionRepository.save(new FileVersion(file, null, 1, "class Main {}", "first save"));

    Long fileId = file.getId();

    projectFileService.deleteFile(project.getPublicId(), owner.getId(), fileId);

    assertThat(projectFileRepository.findById(fileId)).isEmpty();
    assertThat(fileVersionRepository.existsByFileId(fileId)).isFalse();
  }

  @Test
  void deleteFolderAlsoDeletesChildFileVersions() {
    Project project =
        projectRepository.save(new Project("project", "description", ProjectVisibility.PRIVATE));
    User owner = userRepository.save(new User("delete-folder-owner@test.com", "password", "owner"));
    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));

    ProjectFile folder =
        projectFileRepository.save(new ProjectFile(project, null, "src", FileType.FOLDER));
    ProjectFile file =
        projectFileRepository.save(new ProjectFile(project, folder, "Main.java", FileType.FILE));
    fileVersionRepository.save(new FileVersion(file, null, 1, "class Main {}", "first save"));

    Long folderId = folder.getId();
    Long fileId = file.getId();

    projectFileService.deleteFile(project.getPublicId(), owner.getId(), folderId);

    assertThat(projectFileRepository.findById(folderId)).isEmpty();
    assertThat(projectFileRepository.findById(fileId)).isEmpty();
    assertThat(fileVersionRepository.existsByFileId(fileId)).isFalse();
  }
}
