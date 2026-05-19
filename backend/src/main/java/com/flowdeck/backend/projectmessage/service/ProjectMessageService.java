package com.flowdeck.backend.projectmessage.service;

import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import com.flowdeck.backend.global.transaction.AfterCommitExecutor;
import com.flowdeck.backend.permission.service.PermissionService;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.projectmessage.domain.ProjectMessage;
import com.flowdeck.backend.projectmessage.dto.ProjectMessageCreateRequest;
import com.flowdeck.backend.projectmessage.dto.ProjectMessageResponse;
import com.flowdeck.backend.projectmessage.realtime.ProjectMessageBroadcaster;
import com.flowdeck.backend.projectmessage.repository.ProjectMessageRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProjectMessageService {

  private final ProjectRepository projectRepository;
  private final ProjectMessageRepository projectMessageRepository;
  private final ProjectMessageBroadcaster projectMessageBroadcaster;
  private final AfterCommitExecutor afterCommitExecutor;
  private final PermissionService permissionService;

  public ProjectMessageService(
      ProjectRepository projectRepository,
      ProjectMessageRepository projectMessageRepository,
      ProjectMessageBroadcaster projectMessageBroadcaster,
      AfterCommitExecutor afterCommitExecutor,
      PermissionService permissionService) {
    this.projectRepository = projectRepository;
    this.projectMessageRepository = projectMessageRepository;
    this.projectMessageBroadcaster = projectMessageBroadcaster;
    this.afterCommitExecutor = afterCommitExecutor;
    this.permissionService = permissionService;
  }

  @Transactional(readOnly = true)
  public List<ProjectMessageResponse> getMessages(String projectId, Long userId) {
    permissionService.validateProjectAccess(projectId, userId);
    Project project = getProjectByPublicId(projectId);

    return projectMessageRepository.findByProjectIdOrderByCreatedAtAsc(project.getId()).stream()
        .map(ProjectMessageResponse::from)
        .toList();
  }

  @Transactional
  public ProjectMessageResponse createMessage(
      String projectId, Long userId, ProjectMessageCreateRequest request) {
    permissionService.validateEditor(projectId, userId);
    Project project = getProjectByPublicId(projectId);
    ProjectMessage message = ProjectMessage.chat(project, userId, request.content().trim());
    ProjectMessageResponse response =
        ProjectMessageResponse.from(projectMessageRepository.save(message));
    afterCommitExecutor.run(() -> projectMessageBroadcaster.broadcastCreated(projectId, response));
    return response;
  }

  @Transactional(readOnly = true)
  public List<ProjectMessageResponse> searchMessages(
      String projectId, Long userId, String keyword) {
    if (!StringUtils.hasText(keyword)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    permissionService.validateProjectAccess(projectId, userId);
    Project project = getProjectByPublicId(projectId);

    return projectMessageRepository
        .findByProjectIdAndContentContainingIgnoreCaseOrderByCreatedAtAsc(
            project.getId(), keyword.trim())
        .stream()
        .map(ProjectMessageResponse::from)
        .toList();
  }

  @Transactional
  public void deleteMessage(String projectId, Long messageId, Long userId) {
    permissionService.validateProjectAccess(projectId, userId);
    Project project = getProjectByPublicId(projectId);
    ProjectMessage message =
        projectMessageRepository
            .findByIdAndProjectId(messageId, project.getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

    if (!message.isWrittenBy(userId)) {
      throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    projectMessageRepository.delete(message);
    afterCommitExecutor.run(() -> projectMessageBroadcaster.broadcastDeleted(projectId, messageId));
  }

  private Project getProjectByPublicId(String projectId) {
    return projectRepository
        .findByPublicId(projectId)
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
  }
}
