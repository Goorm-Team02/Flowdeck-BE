package com.flowdeck.backend.file.service;

import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.file.dto.ProjectFileCreateRequest;
import com.flowdeck.backend.file.dto.ProjectFileDetailResponse;
import com.flowdeck.backend.file.dto.ProjectFileEventResponse;
import com.flowdeck.backend.file.dto.ProjectFileMoveRequest;
import com.flowdeck.backend.file.dto.ProjectFileRenameRequest;
import com.flowdeck.backend.file.dto.ProjectFileResponse;
import com.flowdeck.backend.file.dto.ProjectFileSearchResponse;
import com.flowdeck.backend.file.dto.ProjectFileTreeResponse;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectFileService {

  private final ProjectRepository projectRepository;
  private final ProjectFileRepository projectFileRepository;
  private final ProjectFileDeletionService projectFileDeletionService;
  private final PermissionService permissionService;
  private final UserRepository userRepository;
  private final ProjectFileBroadcaster projectFileBroadcaster;
  private final AfterCommitExecutor afterCommitExecutor;

  public ProjectFileService(
      ProjectRepository projectRepository,
      ProjectFileRepository projectFileRepository,
      ProjectFileDeletionService projectFileDeletionService,
      PermissionService permissionService,
      UserRepository userRepository,
      ProjectFileBroadcaster projectFileBroadcaster,
      AfterCommitExecutor afterCommitExecutor) {
    this.projectRepository = projectRepository;
    this.projectFileRepository = projectFileRepository;
    this.projectFileDeletionService = projectFileDeletionService;
    this.permissionService = permissionService;
    this.userRepository = userRepository;
    this.projectFileBroadcaster = projectFileBroadcaster;
    this.afterCommitExecutor = afterCommitExecutor;
  }

  @Transactional
  public ProjectFileResponse createFile(
      String projectId, Long userId, ProjectFileCreateRequest request) {
    permissionService.validateEditor(projectId, userId);

    Project project = getProject(projectId);
    ProjectFile parent = getParent(project, request.getParentId());

    validateDuplicateName(project, parent, request.getName());

    ProjectFile file = new ProjectFile(project, parent, request.getName(), request.getType());
    ProjectFile savedFile = projectFileRepository.save(file);

    return ProjectFileResponse.from(savedFile);
  }

  @Transactional(readOnly = true)
  public List<ProjectFileTreeResponse> getFileTree(String projectId, Long userId) {
    permissionService.validateProjectAccess(projectId, userId);

    Project project = getProject(projectId);
    List<ProjectFile> files =
        projectFileRepository.findAllByProjectOrderByParentIdAscNameAsc(project);

    Map<Long, ProjectFileTreeResponse> responseMap = new LinkedHashMap<>();
    List<ProjectFileTreeResponse> roots = new ArrayList<>();

    for (ProjectFile file : files) {
      responseMap.put(file.getId(), ProjectFileTreeResponse.from(file));
    }

    for (ProjectFile file : files) {
      ProjectFileTreeResponse response = responseMap.get(file.getId());

      if (file.getParent() == null) {
        roots.add(response);
        continue;
      }

      ProjectFileTreeResponse parent = responseMap.get(file.getParent().getId());
      if (parent != null) {
        parent.addChild(response);
      }
    }

    return roots;
  }

  @Transactional(readOnly = true)
  public ProjectFileDetailResponse getFile(String projectId, Long userId, Long fileId) {
    permissionService.validateProjectAccess(projectId, userId);

    Project project = getProject(projectId);
    ProjectFile file = getFile(project, fileId);

    if (!file.isFile()) {
      throw new BusinessException(ErrorCode.FILE_INVALID_TYPE);
    }

    return ProjectFileDetailResponse.from(file, file.getCurrentContent());
  }

  @Transactional(readOnly = true)
  public List<ProjectFileSearchResponse> searchFiles(
      String projectId, Long userId, String keyword) {
    permissionService.validateProjectAccess(projectId, userId);

    if (keyword == null || keyword.isBlank()) {
      throw new BusinessException(ErrorCode.FILE_INVALID_KEYWORD);
    }

    Project project = getProject(projectId);
    String normalizedKeyword = keyword.trim();

    return projectFileRepository
        .findAllByProjectAndNameContainingIgnoreCaseOrderByNameAsc(project, normalizedKeyword)
        .stream()
        .map(ProjectFileSearchResponse::from)
        .toList();
  }

  @Transactional
  public ProjectFileResponse renameFile(
      String projectId, Long userId, Long fileId, ProjectFileRenameRequest request) {
    permissionService.validateEditor(projectId, userId);

    Project project = getProject(projectId);
    ProjectFile file = getFile(project, fileId);
    String oldName = file.getName();

    validateDuplicateName(project, file.getParent(), request.getName());

    file.rename(request.getName());
    afterCommitExecutor.run(
        () ->
            projectFileBroadcaster.broadcast(
                ProjectFileEventResponse.renamed(
                    projectId, file, userId, getActorName(userId), oldName)));
    return ProjectFileResponse.from(file);
  }

  @Transactional
  public ProjectFileResponse moveFile(
      String projectId, Long userId, Long fileId, ProjectFileMoveRequest request) {
    permissionService.validateEditor(projectId, userId);

    Project project = getProject(projectId);
    ProjectFile file = getFile(project, fileId);
    Long oldParentId = file.getParent() == null ? null : file.getParent().getId();
    ProjectFile newParent = getParent(project, request.getParentId());

    validateMoveTarget(file, newParent);
    validateDuplicateName(project, newParent, file.getName());

    file.move(newParent);
    afterCommitExecutor.run(
        () ->
            projectFileBroadcaster.broadcast(
                ProjectFileEventResponse.moved(
                    projectId, file, userId, getActorName(userId), oldParentId)));
    return ProjectFileResponse.from(file);
  }

  @Transactional
  public void deleteFile(String projectId, Long userId, Long fileId, Long expectedRevision) {
    permissionService.validateEditor(projectId, userId);

    Project project = getProject(projectId);
    ProjectFile file = getFile(project, fileId);

    validateExpectedRevision(file, expectedRevision);

    List<Long> deletedFileIds = projectFileDeletionService.collectDeletedFileIds(file);
    projectFileDeletionService.deleteRecursive(file);
    afterCommitExecutor.run(
        () ->
            projectFileBroadcaster.broadcast(
                ProjectFileEventResponse.deleted(
                    projectId, file, userId, getActorName(userId), deletedFileIds)));
  }

  private void validateExpectedRevision(ProjectFile file, Long expectedRevision) {
    if (expectedRevision == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    if (file.getEditRevision() != expectedRevision) {
      throw new BusinessException(
          ErrorCode.FILE_EDIT_CONFLICT, FileConflictResponse.from(file, expectedRevision));
    }
  }

  private Project getProject(String projectId) {
    return projectRepository
        .findByPublicId(projectId)
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private ProjectFile getFile(Project project, Long fileId) {
    return projectFileRepository
        .findByIdAndProject(fileId, project)
        .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
  }

  private ProjectFile getParent(Project project, Long parentId) {
    if (parentId == null) {
      return null;
    }

    ProjectFile parent = getFile(project, parentId);
    if (!parent.isFolder()) {
      throw new BusinessException(ErrorCode.FILE_INVALID_TYPE);
    }

    return parent;
  }

  private void validateDuplicateName(Project project, ProjectFile parent, String name) {
    boolean exists =
        parent == null
            ? projectFileRepository.existsByProjectAndParentIsNullAndName(project, name)
            : projectFileRepository.existsByProjectAndParentAndName(project, parent, name);

    if (exists) {
      throw new BusinessException(ErrorCode.FILE_NAME_DUPLICATED);
    }
  }

  private void validateMoveTarget(ProjectFile file, ProjectFile newParent) {
    if (newParent == null) {
      return;
    }

    if (file.getId().equals(newParent.getId())) {
      throw new BusinessException(ErrorCode.FILE_INVALID_MOVE_TARGET);
    }

    ProjectFile current = newParent.getParent();
    while (current != null) {
      if (file.getId().equals(current.getId())) {
        throw new BusinessException(ErrorCode.FILE_INVALID_MOVE_TARGET);
      }
      current = current.getParent();
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
