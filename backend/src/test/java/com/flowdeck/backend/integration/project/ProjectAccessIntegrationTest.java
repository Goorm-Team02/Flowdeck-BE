package com.flowdeck.backend.integration.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.domain.ProjectVisibility;
import com.flowdeck.backend.project.dto.ProjectListResponse;
import com.flowdeck.backend.project.dto.ProjectResponse;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.project.service.ProjectService;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import com.flowdeck.testsupport.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@DatabaseIntegrationTest
@Transactional
class ProjectAccessIntegrationTest {

  private final ProjectRepository projectRepository;
  private final ProjectService projectService;
  private final UserRepository userRepository;

  @Autowired
  ProjectAccessIntegrationTest(
      ProjectRepository projectRepository,
      ProjectService projectService,
      UserRepository userRepository) {
    this.projectRepository = projectRepository;
    this.projectService = projectService;
    this.userRepository = userRepository;
  }

  @Test
  void publicProjectCanBeReadByAuthenticatedNonMember() {
    Project project =
        projectRepository.save(
            new Project("public project", "description", ProjectVisibility.PUBLIC));
    User user = userRepository.save(new User("reader@test.com", "password", "reader"));

    ProjectResponse response = projectService.getPublicProject(project.getPublicId(), user.getId());

    assertThat(response.id()).isEqualTo(project.getPublicId());
  }

  @Test
  void privateProjectCannotBeReadThroughPublicEndpoint() {
    Project project =
        projectRepository.save(
            new Project("private project", "description", ProjectVisibility.PRIVATE));
    User user = userRepository.save(new User("reader2@test.com", "password", "reader"));

    assertThatThrownBy(() -> projectService.getPublicProject(project.getPublicId(), user.getId()))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ACCESS_DENIED);
  }

  @Test
  void publicProjectsRequireAuthenticatedUser() {
    Project project =
        projectRepository.save(
            new Project("public project", "description", ProjectVisibility.PUBLIC));
    User user = userRepository.save(new User("reader3@test.com", "password", "reader"));

    ProjectListResponse response = projectService.getPublicProjects(user.getId());

    assertThat(response.projects()).extracting(ProjectResponse::id).contains(project.getPublicId());
  }

  @Test
  void publicProjectsRejectUnknownUser() {
    assertThatThrownBy(() -> projectService.getPublicProjects(999L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.UNAUTHORIZED);
  }
}
