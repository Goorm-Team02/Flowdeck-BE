package com.flowdeck.backend.integration.project;

import static org.assertj.core.api.Assertions.assertThat;

import com.flowdeck.backend.file.domain.FileType;
import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.file.repository.ProjectFileRepository;
import com.flowdeck.backend.member.domain.ProjectMember;
import com.flowdeck.backend.member.domain.ProjectRole;
import com.flowdeck.backend.member.repository.ProjectMemberRepository;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.domain.ProjectVisibility;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.project.service.ProjectService;
import com.flowdeck.backend.projectmessage.domain.ProjectMessage;
import com.flowdeck.backend.projectmessage.repository.ProjectMessageRepository;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import com.flowdeck.backend.version.domain.FileVersion;
import com.flowdeck.backend.version.repository.FileVersionRepository;
import com.flowdeck.testsupport.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@DatabaseIntegrationTest
@Transactional
class ProjectDeletionTest {

  private final ProjectRepository projectRepository;
  private final ProjectFileRepository projectFileRepository;
  private final FileVersionRepository fileVersionRepository;
  private final ProjectService projectService;
  private final UserRepository userRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final ProjectMessageRepository projectMessageRepository;

  @Autowired
  ProjectDeletionTest(
      ProjectRepository projectRepository,
      ProjectFileRepository projectFileRepository,
      FileVersionRepository fileVersionRepository,
      UserRepository userRepository,
      ProjectMemberRepository projectMemberRepository,
      ProjectMessageRepository projectMessageRepository,
      ProjectService projectService) {
    this.projectRepository = projectRepository;
    this.projectFileRepository = projectFileRepository;
    this.fileVersionRepository = fileVersionRepository;
    this.projectService = projectService;
    this.userRepository = userRepository;
    this.projectMemberRepository = projectMemberRepository;
    this.projectMessageRepository = projectMessageRepository;
  }

  @Test
  void deleteProjectAlsoDeletesFilesAndVersions() {
    Project project =
        projectRepository.save(new Project("project", "description", ProjectVisibility.PRIVATE));
    User owner = userRepository.save(new User("owner@test.com", "password", "owner"));
    ProjectMember member =
        projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));
    ProjectFile folder =
        projectFileRepository.save(new ProjectFile(project, null, "src", FileType.FOLDER));
    ProjectFile file =
        projectFileRepository.save(new ProjectFile(project, folder, "Main.java", FileType.FILE));
    fileVersionRepository.save(new FileVersion(file, null, 1, "class Main {}", "first save"));
    projectMessageRepository.save(ProjectMessage.log(project, owner.getId(), "프로젝트 생성"));

    Long projectId = project.getId();
    Long folderId = folder.getId();
    Long fileId = file.getId();
    Long memberId = member.getId();
    String projectPublicId = project.getPublicId();

    projectService.deleteProject(projectPublicId, owner.getId());

    assertThat(projectRepository.findById(projectId)).isEmpty();
    assertThat(projectFileRepository.findById(folderId)).isEmpty();
    assertThat(projectFileRepository.findById(fileId)).isEmpty();
    assertThat(fileVersionRepository.existsByFileId(fileId)).isFalse();
    assertThat(projectMemberRepository.findById(memberId)).isEmpty();
    assertThat(projectMessageRepository.findByProjectIdOrderByCreatedAtAsc(projectId)).isEmpty();
  }
}
