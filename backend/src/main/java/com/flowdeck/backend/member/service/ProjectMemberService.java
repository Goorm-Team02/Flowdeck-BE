package com.flowdeck.backend.member.service;

import com.flowdeck.backend.auth.service.AuthTokenService;
import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import com.flowdeck.backend.global.security.jwt.JwtProperties;
import com.flowdeck.backend.global.transaction.AfterCommitExecutor;
import com.flowdeck.backend.member.domain.ProjectMember;
import com.flowdeck.backend.member.domain.ProjectRole;
import com.flowdeck.backend.member.dto.MemberInviteRequest;
import com.flowdeck.backend.member.dto.MemberListResponse;
import com.flowdeck.backend.member.dto.MemberResponse;
import com.flowdeck.backend.member.dto.MemberRoleChangedEventResponse;
import com.flowdeck.backend.member.dto.MemberRoleUpdateRequest;
import com.flowdeck.backend.member.realtime.ProjectMemberBroadcaster;
import com.flowdeck.backend.member.repository.ProjectMemberRepository;
import com.flowdeck.backend.permission.service.PermissionService;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.projectmessage.service.ProjectMessageService;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectMemberService {

  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final PermissionService permissionService;
  private final AuthTokenService authTokenService;
  private final JwtProperties jwtProperties;
  private final ProjectMessageService projectMessageService;
  private final ProjectMemberBroadcaster projectMemberBroadcaster;
  private final AfterCommitExecutor afterCommitExecutor;

  public ProjectMemberService(
      ProjectRepository projectRepository,
      UserRepository userRepository,
      ProjectMemberRepository projectMemberRepository,
      PermissionService permissionService,
      AuthTokenService authTokenService,
      JwtProperties jwtProperties,
      ProjectMessageService projectMessageService,
      ProjectMemberBroadcaster projectMemberBroadcaster,
      AfterCommitExecutor afterCommitExecutor) {
    this.projectRepository = projectRepository;
    this.userRepository = userRepository;
    this.projectMemberRepository = projectMemberRepository;
    this.permissionService = permissionService;
    this.authTokenService = authTokenService;
    this.jwtProperties = jwtProperties;
    this.projectMessageService = projectMessageService;
    this.projectMemberBroadcaster = projectMemberBroadcaster;
    this.afterCommitExecutor = afterCommitExecutor;
  }

  @Transactional(readOnly = true)
  public MemberListResponse getMembers(String projectId, Long requesterId) {
    permissionService.validateProjectAccess(projectId, requesterId);

    Project project = getProject(projectId);
    List<MemberResponse> members =
        projectMemberRepository.findAllByProjectOrderByJoinedAtAsc(project).stream()
            .map(MemberResponse::from)
            .toList();

    return MemberListResponse.from(members);
  }

  @Transactional
  public MemberResponse inviteMember(
      String projectId, Long requesterId, MemberInviteRequest request) {
    permissionService.validateOwner(projectId, requesterId);

    Project project = getProject(projectId);
    User user =
        userRepository
            .findByEmail(request.getEmail())
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

    if (projectMemberRepository.existsByProjectAndUser(project, user)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    ProjectMember member = new ProjectMember(project, user, request.getRole());
    ProjectMember savedMember = projectMemberRepository.save(member);
    User requester = getUser(requesterId);
    projectMessageService.createLogMessage(
        projectId,
        requesterId,
        requester.getName() + "님이 " + user.getName() + "님을 " + request.getRole() + " 권한으로 초대했습니다.");

    return MemberResponse.from(savedMember);
  }

  @Transactional
  public MemberResponse updateMemberRole(
      String projectId, Long requesterId, Long memberId, MemberRoleUpdateRequest request) {
    permissionService.validateOwner(projectId, requesterId);

    Project project = getProject(projectId);
    ProjectMember member = getMember(project, memberId);
    User requester = getUser(requesterId);
    ProjectRole previousRole = member.getRole();

    if (member.isOwner() && request.getRole() != ProjectRole.OWNER && isLastOwner(project)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    member.updateRole(request.getRole());
    authTokenService.deleteRefreshToken(member.getUser().getId());
    authTokenService.forceLogout(member.getUser().getId(), forceLogoutTtl());
    MemberRoleChangedEventResponse event =
        MemberRoleChangedEventResponse.of(
            projectId,
            member.getId(),
            member.getUser().getPublicId(),
            previousRole,
            request.getRole(),
            requesterId,
            requester.getName(),
            Instant.now());
    afterCommitExecutor.run(
        () -> projectMemberBroadcaster.broadcastRoleChanged(member.getUser().getEmail(), event));
    projectMessageService.createLogMessage(
        projectId,
        requesterId,
        requester.getName()
            + "님이 "
            + member.getUser().getName()
            + "님의 권한을 "
            + previousRole
            + "에서 "
            + request.getRole()
            + "(으)로 변경했습니다.");

    return MemberResponse.from(member);
  }

  @Transactional
  public void removeMember(String projectId, Long requesterId, Long memberId) {
    permissionService.validateOwner(projectId, requesterId);

    Project project = getProject(projectId);
    ProjectMember member = getMember(project, memberId);
    User requester = getUser(requesterId);

    if (member.isOwner() && isLastOwner(project)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    projectMemberRepository.delete(member);
    authTokenService.deleteRefreshToken(member.getUser().getId());
    authTokenService.forceLogout(member.getUser().getId(), forceLogoutTtl());
    projectMessageService.createLogMessage(
        projectId,
        requesterId,
        requester.getName() + "님이 " + member.getUser().getName() + "님을 프로젝트에서 제거했습니다.");
  }

  @Transactional
  public void leaveProject(String projectId, Long userId) {
    ProjectMember member = permissionService.findMember(projectId, userId);
    Project project = member.getProject();

    if (member.isOwner() && isLastOwner(project)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    projectMemberRepository.delete(member);
    authTokenService.deleteRefreshToken(userId);
    authTokenService.forceLogout(userId, forceLogoutTtl());
    projectMessageService.createLogMessage(
        projectId, userId, member.getUser().getName() + "님이 프로젝트에서 나갔습니다.");
  }

  private boolean isLastOwner(Project project) {
    return projectMemberRepository.countByProjectAndRole(project, ProjectRole.OWNER) <= 1;
  }

  private Duration forceLogoutTtl() {
    return Duration.ofSeconds(jwtProperties.getAccessTokenExpirationSeconds());
  }

  private Project getProject(String projectId) {
    return projectRepository
        .findByPublicId(projectId)
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private User getUser(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
  }

  private ProjectMember getMember(Project project, Long memberId) {
    return projectMemberRepository
        .findByIdAndProject(memberId, project)
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
  }
}
