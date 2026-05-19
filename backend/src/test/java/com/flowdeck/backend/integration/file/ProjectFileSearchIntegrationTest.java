package com.flowdeck.backend.integration.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.flowdeck.backend.file.domain.FileType;
import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.file.dto.ProjectFileSearchResponse;
import com.flowdeck.backend.file.repository.ProjectFileRepository;
import com.flowdeck.backend.file.service.ProjectFileService;
import com.flowdeck.backend.global.error.BusinessException;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@DatabaseIntegrationTest
@Transactional
class ProjectFileSearchIntegrationTest {

  private final ProjectRepository projectRepository;
  private final ProjectFileRepository projectFileRepository;
  private final ProjectFileService projectFileService;
  private final UserRepository userRepository;
  private final ProjectMemberRepository projectMemberRepository;

  @Autowired
  ProjectFileSearchIntegrationTest(
      ProjectRepository projectRepository,
      ProjectFileRepository projectFileRepository,
      ProjectFileService projectFileService,
      UserRepository userRepository,
      ProjectMemberRepository projectMemberRepository) {
    this.projectRepository = projectRepository;
    this.projectFileRepository = projectFileRepository;
    this.projectFileService = projectFileService;
    this.userRepository = userRepository;
    this.projectMemberRepository = projectMemberRepository;
  }

  @Test
  void searchFilesFindsOnlyFilesInSameProjectIgnoringCase() {
    Project project =
        projectRepository.save(new Project("project", "description", ProjectVisibility.PRIVATE));
    Project otherProject =
        projectRepository.save(
            new Project("other project", "description", ProjectVisibility.PRIVATE));

    User owner = userRepository.save(new User("search-owner@test.com", "password", "owner"));
    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));

    ProjectFile mainFile =
        projectFileRepository.save(new ProjectFile(project, null, "Main.java", FileType.FILE));
    projectFileRepository.save(new ProjectFile(project, null, "README.md", FileType.FILE));
    projectFileRepository.save(new ProjectFile(otherProject, null, "Main.java", FileType.FILE));

    List<ProjectFileSearchResponse> responses =
        projectFileService.searchFiles(project.getPublicId(), owner.getId(), "main");

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).fileId()).isEqualTo(mainFile.getId());
    assertThat(responses.get(0).name()).isEqualTo("Main.java");
  }

  @Test
  void searchFilesThrowsExceptionWhenKeywordIsBlank() {
    Project project =
        projectRepository.save(new Project("project", "description", ProjectVisibility.PRIVATE));

    User owner = userRepository.save(new User("blank-search-owner@test.com", "password", "owner"));
    projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));

    assertThatThrownBy(
            () -> projectFileService.searchFiles(project.getPublicId(), owner.getId(), "   "))
        .isInstanceOf(BusinessException.class);
  }
}
