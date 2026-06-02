package com.flowdeck.backend.fileediting.service;

import com.flowdeck.backend.file.domain.FileType;
import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.file.repository.ProjectFileRepository;
import com.flowdeck.backend.fileediting.domain.FileEditingSession;
import com.flowdeck.backend.fileediting.dto.FileEditingResponse;
import com.flowdeck.backend.fileediting.store.FileEditingStore;
import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import com.flowdeck.backend.permission.service.PermissionService;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class FileEditingService {

  private final FileEditingStore fileEditingStore;
  private final PermissionService permissionService;
  private final ProjectRepository projectRepository;
  private final ProjectFileRepository projectFileRepository;
  private final UserRepository userRepository;

  public FileEditingService(
      FileEditingStore fileEditingStore,
      PermissionService permissionService,
      ProjectRepository projectRepository,
      ProjectFileRepository projectFileRepository,
      UserRepository userRepository) {
    this.fileEditingStore = fileEditingStore;
    this.permissionService = permissionService;
    this.projectRepository = projectRepository;
    this.projectFileRepository = projectFileRepository;
    this.userRepository = userRepository;
  }

  public FileEditingResponse startEditing(
      String projectId, Long fileId, Long userId, String sessionId) {
    permissionService.validateEditor(projectId, userId);
    validateFile(projectId, fileId);

    Instant now = Instant.now();
    Optional<FileEditingSession> existingSession = fileEditingStore.findSession(projectId, fileId);
    if (existingSession.isPresent()
        && !canTouch(existingSession.orElseThrow(), userId, sessionId)) {
      return toResponse(existingSession.orElseThrow(), now);
    }

    User user = getUser(userId);
    FileEditingSession session =
        new FileEditingSession(projectId, fileId, userId, user.getName(), sessionId, now);
    fileEditingStore.touchSession(session);
    return toResponse(session, now);
  }

  public FileEditingResponse heartbeatEditing(
      String projectId, Long fileId, Long userId, String sessionId) {
    permissionService.validateEditor(projectId, userId);
    validateFile(projectId, fileId);

    Instant now = Instant.now();
    Optional<FileEditingSession> existingSession = fileEditingStore.findSession(projectId, fileId);
    if (existingSession.isEmpty()) {
      return FileEditingResponse.empty(projectId, fileId, now);
    }

    FileEditingSession session = existingSession.orElseThrow();
    if (!canTouch(session, userId, sessionId)) {
      return toResponse(session, now);
    }

    FileEditingSession refreshedSession =
        new FileEditingSession(
            projectId, fileId, session.userId(), session.userName(), session.sessionId(), now);
    fileEditingStore.touchSession(refreshedSession);
    return toResponse(refreshedSession, now);
  }

  public FileEditingResponse stopEditing(
      String projectId, Long fileId, Long userId, String sessionId) {
    permissionService.validateProjectAccess(projectId, userId);
    validateFile(projectId, fileId);

    fileEditingStore.removeSession(projectId, fileId, sessionId);
    return FileEditingResponse.empty(projectId, fileId, Instant.now());
  }

  private boolean canTouch(FileEditingSession session, Long userId, String sessionId) {
    return userId.equals(session.userId()) || sessionId.equals(session.sessionId());
  }

  private FileEditingResponse toResponse(FileEditingSession session, Instant occurredAt) {
    return FileEditingResponse.editing(
        session.projectId(),
        session.fileId(),
        session.userId(),
        session.userName(),
        session.sessionId(),
        session.lastSeenAt(),
        occurredAt);
  }

  private void validateFile(String projectId, Long fileId) {
    Project project =
        projectRepository
            .findByPublicId(projectId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    ProjectFile file =
        projectFileRepository
            .findByIdAndProject(fileId, project)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    if (file.getType() != FileType.FILE) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  private User getUser(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
  }
}
