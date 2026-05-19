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
import com.flowdeck.backend.user.repository.UserRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
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
  private final UserRepository userRepository;

  public ProjectMessageService(
      ProjectRepository projectRepository,
      ProjectMessageRepository projectMessageRepository,
      ProjectMessageBroadcaster projectMessageBroadcaster,
      AfterCommitExecutor afterCommitExecutor,
      PermissionService permissionService,
      UserRepository userRepository) {
    this.projectRepository = projectRepository;
    this.projectMessageRepository = projectMessageRepository;
    this.projectMessageBroadcaster = projectMessageBroadcaster;
    this.afterCommitExecutor = afterCommitExecutor;
    this.permissionService = permissionService;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<ProjectMessageResponse> getMessages(String projectId, Long userId) {
    permissionService.validateProjectAccess(projectId, userId);
    Project project = getProjectByPublicId(projectId);
    List<ProjectMessage> messages =
        projectMessageRepository.findByProjectIdOrderByCreatedAtAsc(project.getId());

    return toResponses(messages);
  }

  @Transactional
  public ProjectMessageResponse createMessage(
      String projectId, Long userId, ProjectMessageCreateRequest request) {
    permissionService.validateEditor(projectId, userId);
    Project project = getProjectByPublicId(projectId);
    ProjectMessage message = ProjectMessage.chat(project, userId, request.content().trim());
    ProjectMessage savedMessage = projectMessageRepository.save(message);
    ProjectMessageResponse response = toResponse(savedMessage, senderNames(List.of(savedMessage)));
    afterCommitExecutor.run(() -> projectMessageBroadcaster.broadcastCreated(projectId, response));
    return response;
  }

  @Transactional
  public ProjectMessageResponse createLogMessage(String projectId, Long userId, String content) {
    Project project = getProjectByPublicId(projectId);
    ProjectMessage message = ProjectMessage.log(project, userId, content.trim());
    ProjectMessage savedMessage = projectMessageRepository.save(message);
    ProjectMessageResponse response = toResponse(savedMessage, senderNames(List.of(savedMessage)));
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
    List<ProjectMessage> messages =
        projectMessageRepository.findByProjectIdAndContentContainingIgnoreCaseOrderByCreatedAtAsc(
            project.getId(), keyword.trim());

    return toResponses(messages);
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

  private List<ProjectMessageResponse> toResponses(List<ProjectMessage> messages) {
    Map<Long, String> senderNames = senderNames(messages);
    return messages.stream().map(message -> toResponse(message, senderNames)).toList();
  }

  private ProjectMessageResponse toResponse(ProjectMessage message, Map<Long, String> senderNames) {
    return ProjectMessageResponse.from(message, senderNames.get(message.getUserId()));
  }

  private Map<Long, String> senderNames(Collection<ProjectMessage> messages) {
    List<Long> userIds =
        messages.stream()
            .map(ProjectMessage::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

    if (userIds.isEmpty()) {
      return Map.of();
    }

    return userRepository.findAllById(userIds).stream()
        .collect(Collectors.toMap(user -> user.getId(), user -> user.getName()));
  }
}
