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
import com.flowdeck.backend.version.domain.FileVersion;
import com.flowdeck.backend.version.dto.DiffLineResponse;
import com.flowdeck.backend.version.dto.FileConflictResponse;
import com.flowdeck.backend.version.dto.FileVersionCreateRequest;
import com.flowdeck.backend.version.dto.FileVersionCreateResponse;
import com.flowdeck.backend.version.dto.FileVersionDetailResponse;
import com.flowdeck.backend.version.dto.FileVersionDiffResponse;
import com.flowdeck.backend.version.dto.FileVersionListResponse;
import com.flowdeck.backend.version.dto.FileVersionResponse;
import com.flowdeck.backend.version.dto.FileVersionRestoreRequest;
import com.flowdeck.backend.version.dto.FileVersionRestoreResponse;
import com.flowdeck.backend.version.repository.FileVersionRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileVersionService {

  private final ProjectRepository projectRepository;
  private final ProjectFileRepository projectFileRepository;
  private final FileVersionRepository fileVersionRepository;
  private final PermissionService permissionService;
  private final UserRepository userRepository;
  private final ProjectFileBroadcaster projectFileBroadcaster;
  private final AfterCommitExecutor afterCommitExecutor;

  public FileVersionService(
      ProjectRepository projectRepository,
      ProjectFileRepository projectFileRepository,
      FileVersionRepository fileVersionRepository,
      PermissionService permissionService,
      UserRepository userRepository,
      ProjectFileBroadcaster projectFileBroadcaster,
      AfterCommitExecutor afterCommitExecutor) {
    this.projectRepository = projectRepository;
    this.projectFileRepository = projectFileRepository;
    this.fileVersionRepository = fileVersionRepository;
    this.permissionService = permissionService;
    this.userRepository = userRepository;
    this.projectFileBroadcaster = projectFileBroadcaster;
    this.afterCommitExecutor = afterCommitExecutor;
  }

  @Transactional(readOnly = true)
  public FileVersionListResponse getVersions(String projectId, Long userId, Long fileId) {
    permissionService.validateProjectAccess(projectId, userId);

    ProjectFile file = getProjectFile(projectId, fileId);

    List<FileVersionResponse> versions =
        fileVersionRepository.findAllByFileOrderByVersionNumberDesc(file).stream()
            .map(FileVersionResponse::from)
            .toList();

    return FileVersionListResponse.from(versions);
  }

  @Transactional(readOnly = true)
  public FileVersionDetailResponse getVersion(
      String projectId, Long userId, Long fileId, Long versionId) {
    permissionService.validateProjectAccess(projectId, userId);

    ProjectFile file = getProjectFile(projectId, fileId);
    FileVersion version = getFileVersion(file, versionId);

    return FileVersionDetailResponse.from(version);
  }

  @Transactional
  public FileVersionCreateResponse createVersion(
      String projectId, Long userId, Long fileId, FileVersionCreateRequest request) {
    permissionService.validateEditor(projectId, userId);

    ProjectFile file = getProjectFile(projectId, fileId);
    file.increaseVersion();

    FileVersion version =
        new FileVersion(
            file,
            userId,
            file.getCurrentVersion(),
            file.getCurrentContent(),
            request.getChangeMessage());
    fileVersionRepository.save(version);

    return FileVersionCreateResponse.from(file);
  }

  @Transactional
  public FileVersionRestoreResponse restoreVersion(
      String projectId,
      Long userId,
      Long fileId,
      Long versionId,
      FileVersionRestoreRequest request) {
    permissionService.validateEditor(projectId, userId);

    ProjectFile file = getProjectFile(projectId, fileId);
    FileVersion version = getFileVersion(file, versionId);

    if (file.getEditRevision() != request.getBaseRevision()) {
      throw new BusinessException(
          ErrorCode.FILE_EDIT_CONFLICT, FileConflictResponse.from(file, request.getBaseRevision()));
    }

    file.updateContent(version.getContent());
    file.increaseEditRevision();
    file.increaseVersion();

    FileVersion restoredVersion =
        new FileVersion(
            file,
            userId,
            file.getCurrentVersion(),
            version.getContent(),
            "버전 " + version.getVersionNumber() + " 복원");

    fileVersionRepository.save(restoredVersion);
    ProjectFileEventResponse event =
        ProjectFileEventResponse.restored(projectId, file, userId, getActorName(userId));

    afterCommitExecutor.run(() -> projectFileBroadcaster.broadcast(event));

    return FileVersionRestoreResponse.from(file);
  }

  @Transactional(readOnly = true)
  public FileVersionDiffResponse getDiff(
      String projectId, Long userId, Long fileId, int fromVersion, int toVersion) {
    permissionService.validateProjectAccess(projectId, userId);

    ProjectFile file = getProjectFile(projectId, fileId);

    FileVersion from =
        fileVersionRepository
            .findByFileAndVersionNumber(file, fromVersion)
            .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND));
    FileVersion to =
        fileVersionRepository
            .findByFileAndVersionNumber(file, toVersion)
            .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND));

    List<DiffLineResponse> changes = createSimpleDiff(from.getContent(), to.getContent());
    int addedLines = countType(changes, "ADDED");
    int removedLines = countType(changes, "REMOVED");

    return new FileVersionDiffResponse(
        from.getVersionNumber(), to.getVersionNumber(), addedLines, removedLines, changes);
  }

  private ProjectFile getProjectFile(String projectId, Long fileId) {
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

    return file;
  }

  private FileVersion getFileVersion(ProjectFile file, Long versionId) {
    return fileVersionRepository
        .findByFileAndId(file, versionId)
        .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND));
  }

  private List<DiffLineResponse> createSimpleDiff(String oldContent, String newContent) {
    String[] oldLines = oldContent.split("\\R", -1);
    String[] newLines = newContent.split("\\R", -1);

    int maxLength = Math.max(oldLines.length, newLines.length);
    List<DiffLineResponse> changes = new ArrayList<>();

    for (int index = 0; index < maxLength; index++) {
      String oldLine = index < oldLines.length ? oldLines[index] : null;
      String newLine = index < newLines.length ? newLines[index] : null;

      if (oldLine == null) {
        changes.add(new DiffLineResponse("ADDED", null, index + 1, newLine));
      } else if (newLine == null) {
        changes.add(new DiffLineResponse("REMOVED", index + 1, null, oldLine));
      } else if (oldLine.equals(newLine)) {
        changes.add(new DiffLineResponse("UNCHANGED", index + 1, index + 1, newLine));
      } else {
        changes.add(new DiffLineResponse("REMOVED", index + 1, null, oldLine));
        changes.add(new DiffLineResponse("ADDED", null, index + 1, newLine));
      }
    }

    return changes;
  }

  private int countType(List<DiffLineResponse> changes, String type) {
    return (int) changes.stream().filter(change -> type.equals(change.type())).count();
  }

  private String getActorName(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    return user.getName();
  }
}
