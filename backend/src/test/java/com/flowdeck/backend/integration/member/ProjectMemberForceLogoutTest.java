package com.flowdeck.backend.integration.member;

import static org.assertj.core.api.Assertions.assertThat;

import com.flowdeck.backend.auth.service.AuthTokenService;
import com.flowdeck.backend.member.domain.ProjectMember;
import com.flowdeck.backend.member.domain.ProjectRole;
import com.flowdeck.backend.member.dto.MemberInviteRequest;
import com.flowdeck.backend.member.dto.MemberRoleUpdateRequest;
import com.flowdeck.backend.member.repository.ProjectMemberRepository;
import com.flowdeck.backend.member.service.ProjectMemberService;
import com.flowdeck.backend.project.domain.ProjectVisibility;
import com.flowdeck.backend.project.dto.ProjectCreateRequest;
import com.flowdeck.backend.project.service.ProjectService;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import com.flowdeck.testsupport.DatabaseIntegrationTest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@DatabaseIntegrationTest
@ContextConfiguration(classes = ProjectMemberForceLogoutTest.TestAuthTokenConfig.class)
@Transactional
class ProjectMemberForceLogoutTest {

  private final ProjectService projectService;
  private final ProjectMemberService projectMemberService;
  private final ProjectMemberRepository projectMemberRepository;
  private final UserRepository userRepository;
  private final RecordingAuthTokenService authTokenService;

  @Autowired
  ProjectMemberForceLogoutTest(
      ProjectService projectService,
      ProjectMemberService projectMemberService,
      ProjectMemberRepository projectMemberRepository,
      UserRepository userRepository,
      AuthTokenService authTokenService) {
    this.projectService = projectService;
    this.projectMemberService = projectMemberService;
    this.projectMemberRepository = projectMemberRepository;
    this.userRepository = userRepository;
    this.authTokenService = (RecordingAuthTokenService) authTokenService;
  }

  @BeforeEach
  void setUp() {
    authTokenService.clear();
  }

  @Test
  void updateMemberRoleForcesTargetUserLogout() {
    TestProject testProject = createProjectWithOwner("owner1@test.com");
    User editor = userRepository.save(new User("editor1@test.com", "password", "editor"));
    projectMemberService.inviteMember(
        testProject.projectId(), testProject.owner().getId(), invite(editor, ProjectRole.EDITOR));
    ProjectMember editorMember =
        projectMemberRepository
            .findByProjectPublicIdAndUser(testProject.projectId(), editor)
            .orElseThrow();

    projectMemberService.updateMemberRole(
        testProject.projectId(),
        testProject.owner().getId(),
        editorMember.getId(),
        updateRole(ProjectRole.VIEWER));

    assertThat(editorMember.getRole()).isEqualTo(ProjectRole.VIEWER);
    assertThat(authTokenService.forceLogoutUserIds()).containsExactly(editor.getId());
    assertThat(authTokenService.deletedRefreshTokenUserIds()).containsExactly(editor.getId());
  }

  @Test
  void removeMemberForcesTargetUserLogout() {
    TestProject testProject = createProjectWithOwner("owner2@test.com");
    User editor = userRepository.save(new User("editor2@test.com", "password", "editor"));
    projectMemberService.inviteMember(
        testProject.projectId(), testProject.owner().getId(), invite(editor, ProjectRole.EDITOR));
    ProjectMember editorMember =
        projectMemberRepository
            .findByProjectPublicIdAndUser(testProject.projectId(), editor)
            .orElseThrow();

    projectMemberService.removeMember(
        testProject.projectId(), testProject.owner().getId(), editorMember.getId());

    assertThat(projectMemberRepository.findById(editorMember.getId())).isEmpty();
    assertThat(authTokenService.forceLogoutUserIds()).containsExactly(editor.getId());
    assertThat(authTokenService.deletedRefreshTokenUserIds()).containsExactly(editor.getId());
  }

  @Test
  void ownerCanLeaveWhenAnotherOwnerExistsAndForceLogoutIsCalled() {
    TestProject testProject = createProjectWithOwner("owner3@test.com");
    User anotherOwner = userRepository.save(new User("owner4@test.com", "password", "owner"));
    projectMemberService.inviteMember(
        testProject.projectId(),
        testProject.owner().getId(),
        invite(anotherOwner, ProjectRole.OWNER));

    projectMemberService.leaveProject(testProject.projectId(), testProject.owner().getId());

    assertThat(
            projectMemberRepository.findByProjectPublicIdAndUser(
                testProject.projectId(), testProject.owner()))
        .isEmpty();
    assertThat(authTokenService.forceLogoutUserIds()).containsExactly(testProject.owner().getId());
    assertThat(authTokenService.deletedRefreshTokenUserIds())
        .containsExactly(testProject.owner().getId());
  }

  private TestProject createProjectWithOwner(String email) {
    User owner = userRepository.save(new User(email, "password", "owner"));
    String projectId = projectService.createProject(createProjectRequest(), owner.getId()).id();
    return new TestProject(projectId, owner);
  }

  private ProjectCreateRequest createProjectRequest() {
    ProjectCreateRequest request = new ProjectCreateRequest();
    ReflectionTestUtils.setField(request, "title", "project");
    ReflectionTestUtils.setField(request, "description", "description");
    ReflectionTestUtils.setField(request, "visibility", ProjectVisibility.PRIVATE);
    return request;
  }

  private MemberInviteRequest invite(User user, ProjectRole role) {
    MemberInviteRequest request = new MemberInviteRequest();
    ReflectionTestUtils.setField(request, "email", user.getEmail());
    ReflectionTestUtils.setField(request, "role", role);
    return request;
  }

  private MemberRoleUpdateRequest updateRole(ProjectRole role) {
    MemberRoleUpdateRequest request = new MemberRoleUpdateRequest();
    ReflectionTestUtils.setField(request, "role", role);
    return request;
  }

  private record TestProject(String projectId, User owner) {}

  @TestConfiguration
  static class TestAuthTokenConfig {

    @Bean
    @Primary
    RecordingAuthTokenService recordingAuthTokenService() {
      return new RecordingAuthTokenService();
    }
  }

  static class RecordingAuthTokenService extends AuthTokenService {

    private final List<Long> forceLogoutUserIds = new ArrayList<>();
    private final List<Long> deletedRefreshTokenUserIds = new ArrayList<>();

    RecordingAuthTokenService() {
      super(null);
    }

    @Override
    public void forceLogout(Long userId, Duration ttl) {
      forceLogoutUserIds.add(userId);
    }

    @Override
    public void deleteRefreshToken(Long userId) {
      deletedRefreshTokenUserIds.add(userId);
    }

    List<Long> forceLogoutUserIds() {
      return forceLogoutUserIds;
    }

    List<Long> deletedRefreshTokenUserIds() {
      return deletedRefreshTokenUserIds;
    }

    void clear() {
      forceLogoutUserIds.clear();
      deletedRefreshTokenUserIds.clear();
    }
  }
}
