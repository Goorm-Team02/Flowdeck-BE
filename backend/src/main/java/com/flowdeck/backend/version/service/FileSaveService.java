package com.flowdeck.backend.version.service;

import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.file.dto.ProjectFileEventResponse;
import com.flowdeck.backend.file.realtime.ProjectFileBroadcaster;
import com.flowdeck.backend.file.repository.ProjectFileRepository;
import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import com.flowdeck.backend.global.transaction.AfterCommitExecutor;
import com.flowdeck.backend.permission.service.PermissionService;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import com.flowdeck.backend.version.dto.FileConflictResponse;
import com.flowdeck.backend.version.dto.FileSaveRequest;
import com.flowdeck.backend.version.dto.FileSaveResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileSaveService {

  private static final int MAX_FILE_CONTENT_BYTES = 1024 * 1024;

  private final ProjectRepository projectRepository;
  private final ProjectFileRepository projectFileRepository;
  private final PermissionService permissionService;
  private final UserRepository userRepository;
  private final ProjectFileBroadcaster projectFileBroadcaster;
  private final AfterCommitExecutor afterCommitExecutor;

  public FileSaveService(
      ProjectRepository projectRepository,
      ProjectFileRepository projectFileRepository,
      PermissionService permissionService,
      UserRepository userRepository,
      ProjectFileBroadcaster projectFileBroadcaster,
      AfterCommitExecutor afterCommitExecutor) {
    this.projectRepository = projectRepository;
    this.projectFileRepository = projectFileRepository;
    this.permissionService = permissionService;
    this.userRepository = userRepository;
    this.projectFileBroadcaster = projectFileBroadcaster;
    this.afterCommitExecutor = afterCommitExecutor;
  }

  @Transactional
  public FileSaveResponse saveFile(
      String projectId, Long userId, Long fileId, FileSaveRequest request) {
    permissionService.validateEditor(projectId, userId);

    Project project =
        projectRepository
            .findByPublicId(projectId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

    ProjectFile file =
        projectFileRepository
            .findByIdAndProject(fileId, project)
            .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

    if (!file.isFile()) {
      throw new BusinessException(ErrorCode.FILE_INVALID_TYPE);
    }

    if (file.getEditRevision() != request.getBaseRevision()) {
      throw new BusinessException(
          ErrorCode.FILE_EDIT_CONFLICT, FileConflictResponse.from(file, request.getBaseRevision()));
    }

    validateContentSize(request.getContent());

    file.updateContent(request.getContent());
    file.increaseEditRevision();
    afterCommitExecutor.run(
        () ->
            projectFileBroadcaster.broadcast(
                ProjectFileEventResponse.saved(projectId, file, userId, getActorName(userId))));

    return FileSaveResponse.from(file);
  }

  private void validateContentSize(String content) {
    int contentBytes = content.getBytes(StandardCharsets.UTF_8).length;
    if (contentBytes > MAX_FILE_CONTENT_BYTES) {
      throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
    }
  }

  private String getActorName(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    return user.getName();
  }
}
