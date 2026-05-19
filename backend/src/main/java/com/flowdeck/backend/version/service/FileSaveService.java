package com.flowdeck.backend.version.service;

import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.file.repository.ProjectFileRepository;
import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import com.flowdeck.backend.permission.service.PermissionService;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.repository.ProjectRepository;
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

  public FileSaveService(
      ProjectRepository projectRepository,
      ProjectFileRepository projectFileRepository,
      PermissionService permissionService) {
    this.projectRepository = projectRepository;
    this.projectFileRepository = projectFileRepository;
    this.permissionService = permissionService;
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
      throw new BusinessException(ErrorCode.FILE_EDIT_CONFLICT);
    }

    validateContentSize(request.getContent());

    file.updateContent(request.getContent());
    file.increaseEditRevision();

    return FileSaveResponse.from(file);
  }

  private void validateContentSize(String content) {
    int contentBytes = content.getBytes(StandardCharsets.UTF_8).length;
    if (contentBytes > MAX_FILE_CONTENT_BYTES) {
      throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
    }
  }
}
