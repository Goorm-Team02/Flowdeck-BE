package com.flowdeck.backend.unit.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flowdeck.backend.auth.service.AuthTokenService;
import com.flowdeck.backend.global.security.jwt.JwtProperties;
import com.flowdeck.backend.global.transaction.AfterCommitExecutor;
import com.flowdeck.backend.member.domain.ProjectMember;
import com.flowdeck.backend.member.domain.ProjectRole;
import com.flowdeck.backend.member.dto.MemberRoleChangedEventResponse;
import com.flowdeck.backend.member.dto.MemberRoleUpdateRequest;
import com.flowdeck.backend.member.realtime.ProjectMemberBroadcaster;
import com.flowdeck.backend.member.repository.ProjectMemberRepository;
import com.flowdeck.backend.member.service.ProjectMemberService;
import com.flowdeck.backend.permission.service.PermissionService;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.domain.ProjectVisibility;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.projectmessage.service.ProjectMessageService;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class ProjectMemberRoleChangedNotificationTest {

  @Test
  void roleChangedNotificationTargetsUserPublicId() {
    ProjectRepository projectRepository = mock(ProjectRepository.class);
    UserRepository userRepository = mock(UserRepository.class);
    ProjectMemberRepository projectMemberRepository = mock(ProjectMemberRepository.class);
    PermissionService permissionService = mock(PermissionService.class);
    AuthTokenService authTokenService = mock(AuthTokenService.class);
    JwtProperties jwtProperties = mock(JwtProperties.class);
    ProjectMessageService projectMessageService = mock(ProjectMessageService.class);
    ProjectMemberBroadcaster projectMemberBroadcaster = mock(ProjectMemberBroadcaster.class);
    AfterCommitExecutor afterCommitExecutor = mock(AfterCommitExecutor.class);
    ProjectMemberService projectMemberService =
        new ProjectMemberService(
            projectRepository,
            userRepository,
            projectMemberRepository,
            permissionService,
            authTokenService,
            jwtProperties,
            projectMessageService,
            projectMemberBroadcaster,
            afterCommitExecutor);
    Project project = new Project("project", "description", ProjectVisibility.PRIVATE);
    User owner = new User("owner@test.com", "password", "owner");
    User editor = new User("editor@test.com", "password", "editor");
    ProjectMember member = new ProjectMember(project, editor, ProjectRole.EDITOR);
    MemberRoleUpdateRequest request = new MemberRoleUpdateRequest();
    ReflectionTestUtils.setField(request, "role", ProjectRole.VIEWER);
    when(projectRepository.findByPublicId(project.getPublicId())).thenReturn(Optional.of(project));
    when(projectMemberRepository.findByIdAndProject(10L, project)).thenReturn(Optional.of(member));
    when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
    when(jwtProperties.getAccessTokenExpirationSeconds()).thenReturn(1800L);
    doAnswer(
            invocation -> {
              invocation.getArgument(0, Runnable.class).run();
              return null;
            })
        .when(afterCommitExecutor)
        .run(any(Runnable.class));

    projectMemberService.updateMemberRole(project.getPublicId(), 1L, 10L, request);

    ArgumentCaptor<MemberRoleChangedEventResponse> eventCaptor =
        ArgumentCaptor.forClass(MemberRoleChangedEventResponse.class);
    verify(projectMemberBroadcaster)
        .broadcastRoleChanged(
            org.mockito.ArgumentMatchers.eq(editor.getPublicId()), eventCaptor.capture());
    assertThat(eventCaptor.getValue().userId()).isEqualTo(editor.getPublicId());
  }
}
