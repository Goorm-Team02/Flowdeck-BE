package com.flowdeck.backend.integration.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.member.domain.ProjectMember;
import com.flowdeck.backend.member.domain.ProjectRole;
import com.flowdeck.backend.member.dto.MemberInviteRequest;
import com.flowdeck.backend.member.repository.ProjectMemberRepository;
import com.flowdeck.backend.member.service.ProjectMemberService;
import com.flowdeck.backend.permission.service.PermissionService;
import com.flowdeck.backend.project.domain.ProjectVisibility;
import com.flowdeck.backend.project.dto.ProjectCreateRequest;
import com.flowdeck.backend.project.service.ProjectService;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import com.flowdeck.testsupport.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@DatabaseIntegrationTest
@Transactional
class ProjectMemberServiceIntegrationTest {

  private final ProjectService projectService;
  private final ProjectMemberService projectMemberService;
  private final PermissionService permissionService;
  private final ProjectMemberRepository projectMemberRepository;
  private final UserRepository userRepository;

  @Autowired
  ProjectMemberServiceIntegrationTest(
      ProjectService projectService,
      ProjectMemberService projectMemberService,
      PermissionService permissionService,
      ProjectMemberRepository projectMemberRepository,
      UserRepository userRepository) {
    this.projectService = projectService;
    this.projectMemberService = projectMemberService;
    this.permissionService = permissionService;
    this.projectMemberRepository = projectMemberRepository;
    this.userRepository = userRepository;
  }

  @Test
  void createProjectAlsoCreatesOwnerMember() {
    User owner = userRepository.save(new User("owner1@test.com", "password", "owner"));

    String projectId = projectService.createProject(createProjectRequest(), owner.getId()).id();

    ProjectMember member =
        projectMemberRepository.findByProjectPublicIdAndUser(projectId, owner).orElseThrow();

    assertThat(member.getRole()).isEqualTo(ProjectRole.OWNER);
  }

  @Test
  void ownerCanAccessOwnerAndEditorPermissions() {
    User owner = userRepository.save(new User("owner2@test.com", "password", "owner"));

    String projectId = projectService.createProject(createProjectRequest(), owner.getId()).id();

    permissionService.validateProjectAccess(projectId, owner.getId());
    permissionService.validateEditor(projectId, owner.getId());
    permissionService.validateOwner(projectId, owner.getId());
  }

  @Test
  void nonMemberCannotAccessProject() {
    User owner = userRepository.save(new User("owner3@test.com", "password", "owner"));
    User outsider = userRepository.save(new User("outsider@test.com", "password", "outsider"));

    String projectId = projectService.createProject(createProjectRequest(), owner.getId()).id();

    assertThatThrownBy(() -> permissionService.validateProjectAccess(projectId, outsider.getId()))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void lastOwnerCannotLeaveProject() {
    User owner = userRepository.save(new User("owner4@test.com", "password", "owner"));

    String projectId = projectService.createProject(createProjectRequest(), owner.getId()).id();

    assertThatThrownBy(() -> projectMemberService.leaveProject(projectId, owner.getId()))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void ownerCanInviteMember() {
    User owner = userRepository.save(new User("owner5@test.com", "password", "owner"));
    User editor = userRepository.save(new User("editor1@test.com", "password", "editor"));

    String projectId = projectService.createProject(createProjectRequest(), owner.getId()).id();

    projectMemberService.inviteMember(
        projectId, owner.getId(), createInviteRequest(editor.getEmail(), ProjectRole.EDITOR));

    ProjectMember member =
        projectMemberRepository.findByProjectPublicIdAndUser(projectId, editor).orElseThrow();

    assertThat(member.getRole()).isEqualTo(ProjectRole.EDITOR);
  }

  @Test
  void cannotInviteAlreadyJoinedMember() {
    User owner = userRepository.save(new User("owner6@test.com", "password", "owner"));

    String projectId = projectService.createProject(createProjectRequest(), owner.getId()).id();

    assertThatThrownBy(
            () ->
                projectMemberService.inviteMember(
                    projectId,
                    owner.getId(),
                    createInviteRequest(owner.getEmail(), ProjectRole.EDITOR)))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void viewerCanAccessProjectButCannotEdit() {
    User owner = userRepository.save(new User("owner7@test.com", "password", "owner"));
    User viewer = userRepository.save(new User("viewer1@test.com", "password", "viewer"));

    String projectId = projectService.createProject(createProjectRequest(), owner.getId()).id();

    projectMemberService.inviteMember(
        projectId, owner.getId(), createInviteRequest(viewer.getEmail(), ProjectRole.VIEWER));

    permissionService.validateProjectAccess(projectId, viewer.getId());

    assertThatThrownBy(() -> permissionService.validateEditor(projectId, viewer.getId()))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void editorCannotUseOwnerPermission() {
    User owner = userRepository.save(new User("owner8@test.com", "password", "owner"));
    User editor = userRepository.save(new User("editor2@test.com", "password", "editor"));

    String projectId = projectService.createProject(createProjectRequest(), owner.getId()).id();

    projectMemberService.inviteMember(
        projectId, owner.getId(), createInviteRequest(editor.getEmail(), ProjectRole.EDITOR));

    assertThatThrownBy(() -> permissionService.validateOwner(projectId, editor.getId()))
        .isInstanceOf(BusinessException.class);
  }

  private ProjectCreateRequest createProjectRequest() {
    ProjectCreateRequest request = new ProjectCreateRequest();
    ReflectionTestUtils.setField(request, "title", "project");
    ReflectionTestUtils.setField(request, "description", "description");
    ReflectionTestUtils.setField(request, "visibility", ProjectVisibility.PRIVATE);
    return request;
  }

  private MemberInviteRequest createInviteRequest(String email, ProjectRole role) {
    MemberInviteRequest request = new MemberInviteRequest();
    ReflectionTestUtils.setField(request, "email", email);
    ReflectionTestUtils.setField(request, "role", role);
    return request;
  }
}
