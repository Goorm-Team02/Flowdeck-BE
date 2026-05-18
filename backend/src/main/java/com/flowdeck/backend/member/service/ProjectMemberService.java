package com.flowdeck.backend.member.service;

import com.flowdeck.backend.auth.service.AuthTokenService;
import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import com.flowdeck.backend.member.domain.ProjectMember;
import com.flowdeck.backend.member.domain.ProjectRole;
import com.flowdeck.backend.member.dto.MemberInviteRequest;
import com.flowdeck.backend.member.dto.MemberListResponse;
import com.flowdeck.backend.member.dto.MemberResponse;
import com.flowdeck.backend.member.dto.MemberRoleUpdateRequest;
import com.flowdeck.backend.member.repository.ProjectMemberRepository;
import com.flowdeck.backend.permission.service.PermissionService;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectMemberService {

  private static final Duration FORCE_LOGOUT_TTL = Duration.ofMinutes(30);

  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final PermissionService permissionService;
  private final AuthTokenService authTokenService;

  public ProjectMemberService(
      ProjectRepository projectRepository,
      UserRepository userRepository,
      ProjectMemberRepository projectMemberRepository,
      PermissionService permissionService,
      AuthTokenService authTokenService) {
    this.projectRepository = projectRepository;
    this.userRepository = userRepository;
    this.projectMemberRepository = projectMemberRepository;
    this.permissionService = permissionService;
    this.authTokenService = authTokenService;
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

    return MemberResponse.from(savedMember);
  }

  @Transactional
  public MemberResponse updateMemberRole(
      String projectId, Long requesterId, Long memberId, MemberRoleUpdateRequest request) {
    permissionService.validateOwner(projectId, requesterId);

    Project project = getProject(projectId);
    ProjectMember member = getMember(project, memberId);

    if (member.isOwner() && request.getRole() != ProjectRole.OWNER && isLastOwner(project)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    member.updateRole(request.getRole());
    authTokenService.forceLogout(member.getUser().getId(), FORCE_LOGOUT_TTL);

    return MemberResponse.from(member);
  }

  @Transactional
  public void removeMember(String projectId, Long requesterId, Long memberId) {
    permissionService.validateOwner(projectId, requesterId);

    Project project = getProject(projectId);
    ProjectMember member = getMember(project, memberId);

    if (member.isOwner() && isLastOwner(project)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    projectMemberRepository.delete(member);
    authTokenService.forceLogout(member.getUser().getId(), FORCE_LOGOUT_TTL);
  }

  @Transactional
  public void leaveProject(String projectId, Long userId) {
    ProjectMember member = permissionService.findMember(projectId, userId);
    Project project = member.getProject();

    if (member.isOwner() && isLastOwner(project)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    projectMemberRepository.delete(member);
    authTokenService.forceLogout(userId, FORCE_LOGOUT_TTL);
  }

  private boolean isLastOwner(Project project) {
    return projectMemberRepository.countByProjectAndRole(project, ProjectRole.OWNER) <= 1;
  }

  private Project getProject(String projectId) {
    return projectRepository
        .findByPublicId(projectId)
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private ProjectMember getMember(Project project, Long memberId) {
    return projectMemberRepository
        .findByIdAndProject(memberId, project)
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
  }
}
