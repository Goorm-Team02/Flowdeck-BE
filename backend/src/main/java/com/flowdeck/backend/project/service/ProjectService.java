package com.flowdeck.backend.project.service;

import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.domain.ProjectVisibility;
import com.flowdeck.backend.project.dto.ProjectCreateRequest;
import com.flowdeck.backend.project.dto.ProjectListResponse;
import com.flowdeck.backend.project.dto.ProjectResponse;
import com.flowdeck.backend.project.dto.ProjectUpdateRequest;
import com.flowdeck.backend.project.repository.ProjectRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

  private final ProjectRepository projectRepository;

  public ProjectService(ProjectRepository projectRepository) {
    this.projectRepository = projectRepository;
  }

  @Transactional
  public ProjectResponse createProject(ProjectCreateRequest request) {
    Project project =
        new Project(request.getTitle(), request.getDescription(), request.getVisibility());
    Project savedProject = projectRepository.save(project);

    return ProjectResponse.from(savedProject);
  }

  @Transactional(readOnly = true)
  public ProjectResponse getProject(String projectId) {
    Project project = getProjectByPublicId(projectId);
    return ProjectResponse.from(project);
  }

  @Transactional
  public ProjectResponse updateProject(String projectId, ProjectUpdateRequest request) {
    Project project = getProjectByPublicId(projectId);
    project.update(request.getTitle(), request.getDescription(), request.getVisibility());

    return ProjectResponse.from(project);
  }

  @Transactional
  public void deleteProject(String projectId) {
    Project project = getProjectByPublicId(projectId);
    projectRepository.delete(project);
  }

  @Transactional(readOnly = true)
  public ProjectListResponse getPublicProjects() {
    List<ProjectResponse> projects =
        projectRepository.findAllByVisibilityOrderByCreatedAtDesc(ProjectVisibility.PUBLIC).stream()
            .map(ProjectResponse::from)
            .toList();

    return ProjectListResponse.from(projects);
  }

  @Transactional(readOnly = true)
  public ProjectResponse getPublicProject(String projectId) {
    Project project = getProjectByPublicId(projectId);

    if (project.getVisibility() != ProjectVisibility.PUBLIC) {
      throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    return ProjectResponse.from(project);
  }

  private Project getProjectByPublicId(String projectId) {
    return projectRepository
        .findByPublicId(projectId)
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
  }
}
