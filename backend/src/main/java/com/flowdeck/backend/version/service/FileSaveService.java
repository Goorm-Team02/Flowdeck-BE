package com.flowdeck.backend.version.service;

import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.file.repository.ProjectFileRepository;
import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import com.flowdeck.backend.permission.service.PermissionService;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.version.domain.FileVersion;
import com.flowdeck.backend.version.dto.FileSaveRequest;
import com.flowdeck.backend.version.dto.FileSaveResponse;
import com.flowdeck.backend.version.repository.FileVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileSaveService {

  private final ProjectRepository projectRepository;
  private final ProjectFileRepository projectFileRepository;
  private final FileVersionRepository fileVersionRepository;
  private final PermissionService permissionService;

  public FileSaveService(
      ProjectRepository projectRepository,
      ProjectFileRepository projectFileRepository,
      FileVersionRepository fileVersionRepository,
      PermissionService permissionService) {
    this.projectRepository = projectRepository;
    this.projectFileRepository = projectFileRepository;
    this.fileVersionRepository = fileVersionRepository;
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
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

    if (!file.isFile()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    file.increaseVersion();

    FileVersion version =
        new FileVersion(
            file,
            userId,
            file.getCurrentVersion(),
            request.getContent(),
            request.getChangeMessage());
    fileVersionRepository.save(version);

    return FileSaveResponse.from(file);
  }
}
