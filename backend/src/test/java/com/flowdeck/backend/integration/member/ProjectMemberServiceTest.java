package com.flowdeck.backend.integration.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.member.domain.ProjectMember;
import com.flowdeck.backend.member.domain.ProjectRole;
import com.flowdeck.backend.member.dto.MemberInviteRequest;
import com.flowdeck.backend.member.dto.MemberRoleUpdateRequest;
import com.flowdeck.backend.member.repository.ProjectMemberRepository;
import com.flowdeck.backend.member.service.ProjectMemberService;
import com.flowdeck.backend.permission.service.PermissionService;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.domain.ProjectVisibility;
import com.flowdeck.backend.project.dto.ProjectCreateRequest;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.project.service.ProjectService;
import com.flowdeck.backend.projectmessage.domain.ProjectMessage;
import com.flowdeck.backend.projectmessage.domain.ProjectMessageType;
import com.flowdeck.backend.projectmessage.repository.ProjectMessageRepository;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import com.flowdeck.testsupport.DatabaseIntegrationTest;
import com.flowdeck.testsupport.MockAuthTokenServiceConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@DatabaseIntegrationTest
@Transactional
@Import(MockAuthTokenServiceConfig.class)
class ProjectMemberServiceTest {

  private final ProjectService projectService;
  private final ProjectMemberService projectMemberService;
  private final PermissionService permissionService;
  private final ProjectMemberRepository projectMemberRepository;
  private final UserRepository userRepository;
  private final ProjectRepository projectRepository;
  private final ProjectMessageRepository projectMessageRepository;

  @Autowired
  ProjectMemberServiceTest(
      ProjectService projectService,
      ProjectMemberService projectMemberService,
      PermissionService permissionService,
      ProjectMemberRepository projectMemberRepository,
      UserRepository userRepository,
      ProjectRepository projectRepository,
      ProjectMessageRepository projectMessageRepository) {
    this.projectService = projectService;
    this.projectMemberService = projectMemberService;
    this.permissionService = permissionService;
    this.projectMemberRepository = projectMemberRepository;
    this.userRepository = userRepository;
    this.projectRepository = projectRepository;
    this.projectMessageRepository = projectMessageRepository;
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
  void invitingMemberCreatesLogMessage() {
    User owner = userRepository.save(new User("owner9@test.com", "password", "owner"));
    User editor = userRepository.save(new User("editor9@test.com", "password", "editor"));

    String projectId = projectService.createProject(createProjectRequest(), owner.getId()).id();

    projectMemberService.inviteMember(
        projectId, owner.getId(), createInviteRequest(editor.getEmail(), ProjectRole.EDITOR));

    ProjectMessage logMessage = getMessages(projectId).getFirst();
    assertThat(logMessage.getMessageType()).isEqualTo(ProjectMessageType.LOG);
    assertThat(logMessage.getUserId()).isEqualTo(owner.getId());
    assertThat(logMessage.getContent())
        .isEqualTo(owner.getName() + "님이 " + editor.getName() + "님을 EDITOR 권한으로 초대했습니다.");
  }

  @Test
  void updatingMemberRoleCreatesLogMessage() {
    User owner = userRepository.save(new User("owner10@test.com", "password", "owner"));
    User viewer = userRepository.save(new User("viewer10@test.com", "password", "viewer"));

    String projectId = projectService.createProject(createProjectRequest(), owner.getId()).id();
    projectMemberService.inviteMember(
        projectId, owner.getId(), createInviteRequest(viewer.getEmail(), ProjectRole.VIEWER));
    ProjectMember member =
        projectMemberRepository.findByProjectPublicIdAndUser(projectId, viewer).orElseThrow();

    projectMemberService.updateMemberRole(
        projectId, owner.getId(), member.getId(), createRoleUpdateRequest(ProjectRole.EDITOR));

    ProjectMessage logMessage = getMessages(projectId).getLast();
    assertThat(logMessage.getMessageType()).isEqualTo(ProjectMessageType.LOG);
    assertThat(logMessage.getUserId()).isEqualTo(owner.getId());
    assertThat(logMessage.getContent())
        .isEqualTo(
            owner.getName() + "님이 " + viewer.getName() + "님의 권한을 VIEWER에서 EDITOR(으)로 변경했습니다.");
  }

  @Test
  void removingMemberCreatesLogMessage() {
    User owner = userRepository.save(new User("owner11@test.com", "password", "owner"));
    User editor = userRepository.save(new User("editor11@test.com", "password", "editor"));

    String projectId = projectService.createProject(createProjectRequest(), owner.getId()).id();
    projectMemberService.inviteMember(
        projectId, owner.getId(), createInviteRequest(editor.getEmail(), ProjectRole.EDITOR));
    ProjectMember member =
        projectMemberRepository.findByProjectPublicIdAndUser(projectId, editor).orElseThrow();

    projectMemberService.removeMember(projectId, owner.getId(), member.getId());

    ProjectMessage logMessage = getMessages(projectId).getLast();
    assertThat(logMessage.getMessageType()).isEqualTo(ProjectMessageType.LOG);
    assertThat(logMessage.getUserId()).isEqualTo(owner.getId());
    assertThat(logMessage.getContent())
        .isEqualTo(owner.getName() + "님이 " + editor.getName() + "님을 프로젝트에서 제거했습니다.");
  }

  @Test
  void leavingProjectCreatesLogMessage() {
    User owner = userRepository.save(new User("owner12@test.com", "password", "owner"));
    User editor = userRepository.save(new User("editor12@test.com", "password", "editor"));

    String projectId = projectService.createProject(createProjectRequest(), owner.getId()).id();
    projectMemberService.inviteMember(
        projectId, owner.getId(), createInviteRequest(editor.getEmail(), ProjectRole.EDITOR));

    projectMemberService.leaveProject(projectId, editor.getId());

    ProjectMessage logMessage = getMessages(projectId).getLast();
    assertThat(logMessage.getMessageType()).isEqualTo(ProjectMessageType.LOG);
    assertThat(logMessage.getUserId()).isEqualTo(editor.getId());
    assertThat(logMessage.getContent()).isEqualTo(editor.getName() + "님이 프로젝트에서 나갔습니다.");
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

  private MemberRoleUpdateRequest createRoleUpdateRequest(ProjectRole role) {
    MemberRoleUpdateRequest request = new MemberRoleUpdateRequest();
    ReflectionTestUtils.setField(request, "role", role);
    return request;
  }

  private List<ProjectMessage> getMessages(String projectId) {
    Project project = projectRepository.findByPublicId(projectId).orElseThrow();
    return projectMessageRepository.findByProjectIdOrderByCreatedAtAsc(project.getId());
  }
}
