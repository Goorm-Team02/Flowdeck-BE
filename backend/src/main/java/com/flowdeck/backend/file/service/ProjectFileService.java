package com.flowdeck.backend.file.service;

import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.file.dto.ProjectFileCreateRequest;
import com.flowdeck.backend.file.dto.ProjectFileMoveRequest;
import com.flowdeck.backend.file.dto.ProjectFileRenameRequest;
import com.flowdeck.backend.file.dto.ProjectFileResponse;
import com.flowdeck.backend.file.dto.ProjectFileTreeResponse;
import com.flowdeck.backend.file.repository.ProjectFileRepository;
import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.repository.ProjectRepository;
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

  public ProjectFileService(
      ProjectRepository projectRepository, ProjectFileRepository projectFileRepository) {
    this.projectRepository = projectRepository;
    this.projectFileRepository = projectFileRepository;
  }

  @Transactional
  public ProjectFileResponse createFile(String projectId, ProjectFileCreateRequest request) {
    Project project = getProject(projectId);
    ProjectFile parent = getParent(project, request.getParentId());

    validateDuplicateName(project, parent, request.getName());

    ProjectFile file = new ProjectFile(project, parent, request.getName(), request.getType());
    ProjectFile savedFile = projectFileRepository.save(file);

    return ProjectFileResponse.from(savedFile);
  }

  @Transactional(readOnly = true)
  public List<ProjectFileTreeResponse> getFileTree(String projectId) {
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
  public ProjectFileResponse getFile(String projectId, Long fileId) {
    Project project = getProject(projectId);
    ProjectFile file = getFile(project, fileId);

    return ProjectFileResponse.from(file);
  }

  @Transactional
  public ProjectFileResponse renameFile(
      String projectId, Long fileId, ProjectFileRenameRequest request) {
    Project project = getProject(projectId);
    ProjectFile file = getFile(project, fileId);

    validateDuplicateName(project, file.getParent(), request.getName());

    file.rename(request.getName());
    return ProjectFileResponse.from(file);
  }

  @Transactional
  public ProjectFileResponse moveFile(
      String projectId, Long fileId, ProjectFileMoveRequest request) {
    Project project = getProject(projectId);
    ProjectFile file = getFile(project, fileId);
    ProjectFile newParent = getParent(project, request.getParentId());

    validateMoveTarget(file, newParent);
    validateDuplicateName(project, newParent, file.getName());

    file.move(newParent);
    return ProjectFileResponse.from(file);
  }

  @Transactional
  public void deleteFile(String projectId, Long fileId) {
    Project project = getProject(projectId);
    ProjectFile file = getFile(project, fileId);

    deleteRecursive(file);
  }

  private Project getProject(String projectId) {
    return projectRepository
        .findByPublicId(projectId)
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private ProjectFile getFile(Project project, Long fileId) {
    return projectFileRepository
        .findByIdAndProject(fileId, project)
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private ProjectFile getParent(Project project, Long parentId) {
    if (parentId == null) {
      return null;
    }

    ProjectFile parent = getFile(project, parentId);
    if (!parent.isFolder()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    return parent;
  }

  private void validateDuplicateName(Project project, ProjectFile parent, String name) {
    boolean exists =
        parent == null
            ? projectFileRepository.existsByProjectAndParentIsNullAndName(project, name)
            : projectFileRepository.existsByProjectAndParentAndName(project, parent, name);

    if (exists) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  private void validateMoveTarget(ProjectFile file, ProjectFile newParent) {
    if (newParent == null) {
      return;
    }

    if (file.getId().equals(newParent.getId())) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    ProjectFile current = newParent.getParent();
    while (current != null) {
      if (file.getId().equals(current.getId())) {
        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
      }
      current = current.getParent();
    }
  }

  private void deleteRecursive(ProjectFile file) {
    List<ProjectFile> children = projectFileRepository.findAllByParent(file);
    for (ProjectFile child : children) {
      deleteRecursive(child);
    }

    // TODO: FileVersion 도메인 추가 후 파일 버전 삭제 로직 연결
    projectFileRepository.delete(file);
  }
}
