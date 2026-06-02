package com.flowdeck.backend.integration.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import com.flowdeck.backend.member.domain.ProjectMember;
import com.flowdeck.backend.member.domain.ProjectRole;
import com.flowdeck.backend.member.repository.ProjectMemberRepository;
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
class ProjectAccessTest {

  private final ProjectRepository projectRepository;
  private final ProjectService projectService;
  private final ProjectMemberRepository projectMemberRepository;
  private final UserRepository userRepository;

  @Autowired
  ProjectAccessTest(
      ProjectRepository projectRepository,
      ProjectService projectService,
      ProjectMemberRepository projectMemberRepository,
      UserRepository userRepository) {
    this.projectRepository = projectRepository;
    this.projectService = projectService;
    this.projectMemberRepository = projectMemberRepository;
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

  @Test
  void projectsContainOnlyJoinedProjectsInNewestFirstOrderRegardlessOfRole() {
    User user = userRepository.save(new User("member@test.com", "password", "member"));
    User otherUser = userRepository.save(new User("other@test.com", "password", "other"));

    Project ownerProject = saveProject("owner project");
    Project editorProject = saveProject("editor project");
    Project viewerProject = saveProject("viewer project");
    Project otherProject = saveProject("other project");

    projectMemberRepository.save(new ProjectMember(ownerProject, user, ProjectRole.OWNER));
    projectMemberRepository.save(new ProjectMember(editorProject, user, ProjectRole.EDITOR));
    projectMemberRepository.save(new ProjectMember(viewerProject, user, ProjectRole.VIEWER));
    projectMemberRepository.save(new ProjectMember(otherProject, otherUser, ProjectRole.OWNER));

    ProjectListResponse response = projectService.getProjects(user.getId());

    assertThat(response.projects())
        .extracting(ProjectResponse::id)
        .containsExactly(
            viewerProject.getPublicId(), editorProject.getPublicId(), ownerProject.getPublicId());
  }

  @Test
  void projectsRejectUnknownUser() {
    assertThatThrownBy(() -> projectService.getProjects(999L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.UNAUTHORIZED);
  }

  private Project saveProject(String title) {
    return projectRepository.saveAndFlush(
        new Project(title, "description", ProjectVisibility.PRIVATE));
  }
}
