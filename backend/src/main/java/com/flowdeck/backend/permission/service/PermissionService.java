package com.flowdeck.backend.permission.service;

import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import com.flowdeck.backend.member.domain.ProjectMember;
import com.flowdeck.backend.member.repository.ProjectMemberRepository;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissionService {

  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;
  private final ProjectMemberRepository projectMemberRepository;

  public PermissionService(
      ProjectRepository projectRepository,
      UserRepository userRepository,
      ProjectMemberRepository projectMemberRepository) {
    this.projectRepository = projectRepository;
    this.userRepository = userRepository;
    this.projectMemberRepository = projectMemberRepository;
  }

  @Transactional(readOnly = true)
  public void validateProjectAccess(String projectId, Long userId) {
    findMember(projectId, userId);
  }

  @Transactional(readOnly = true)
  public void validateEditor(String projectId, Long userId) {
    ProjectMember member = findMember(projectId, userId);
    if (!member.canEdit()) {
      throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }
  }

  @Transactional(readOnly = true)
  public void validateOwner(String projectId, Long userId) {
    ProjectMember member = findMember(projectId, userId);
    if (!member.isOwner()) {
      throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }
  }

  @Transactional(readOnly = true)
  public ProjectMember findMember(String projectId, Long userId) {
    Project project = getProject(projectId);
    User user = getUser(userId);

    return projectMemberRepository
        .findByProjectAndUser(project, user)
        .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED));
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
}
