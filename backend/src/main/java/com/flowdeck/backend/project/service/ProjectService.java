package com.flowdeck.backend.project.service;

import com.flowdeck.backend.file.service.ProjectFileDeletionService;
import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import com.flowdeck.backend.member.domain.ProjectMember;
import com.flowdeck.backend.member.domain.ProjectRole;
import com.flowdeck.backend.member.repository.ProjectMemberRepository;
import com.flowdeck.backend.permission.service.PermissionService;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.domain.ProjectVisibility;
import com.flowdeck.backend.project.dto.ProjectCreateRequest;
import com.flowdeck.backend.project.dto.ProjectListResponse;
import com.flowdeck.backend.project.dto.ProjectResponse;
import com.flowdeck.backend.project.dto.ProjectUpdateRequest;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.user.domain.User;
import com.flowdeck.backend.user.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

  private final ProjectRepository projectRepository;
  private final ProjectFileDeletionService projectFileDeletionService;
  private final UserRepository userRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final PermissionService permissionService;

  public ProjectService(
      ProjectRepository projectRepository,
      ProjectFileDeletionService projectFileDeletionService,
      UserRepository userRepository,
      ProjectMemberRepository projectMemberRepository,
      PermissionService permissionService) {
    this.projectRepository = projectRepository;
    this.projectFileDeletionService = projectFileDeletionService;
    this.userRepository = userRepository;
    this.projectMemberRepository = projectMemberRepository;
    this.permissionService = permissionService;
  }

  @Transactional
  public ProjectResponse createProject(ProjectCreateRequest request, Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

    Project project =
        new Project(request.getTitle(), request.getDescription(), request.getVisibility());
    Project savedProject = projectRepository.save(project);

    ProjectMember owner = new ProjectMember(savedProject, user, ProjectRole.OWNER);
    projectMemberRepository.save(owner);

    return ProjectResponse.from(savedProject);
  }

  @Transactional(readOnly = true)
  public ProjectResponse getProject(String projectId, Long userId) {
    permissionService.validateProjectAccess(projectId, userId);

    Project project = getProjectByPublicId(projectId);
    return ProjectResponse.from(project);
  }

  @Transactional
  public ProjectResponse updateProject(
      String projectId, Long userId, ProjectUpdateRequest request) {
    permissionService.validateOwner(projectId, userId);

    Project project = getProjectByPublicId(projectId);
    project.update(request.getTitle(), request.getDescription(), request.getVisibility());

    return ProjectResponse.from(project);
  }

  @Transactional
  public void deleteProject(String projectId, Long userId) {
    permissionService.validateOwner(projectId, userId);

    Project project = getProjectByPublicId(projectId);
    projectFileDeletionService.deleteProjectFiles(project);
    projectMemberRepository.deleteAllByProject(project);
    projectRepository.delete(project);
  }

  @Transactional(readOnly = true)
  public ProjectListResponse getPublicProjects(Long userId) {
    validateAuthenticatedUser(userId);

    List<ProjectResponse> projects =
        projectRepository.findAllByVisibilityOrderByCreatedAtDesc(ProjectVisibility.PUBLIC).stream()
            .map(ProjectResponse::from)
            .toList();

    return ProjectListResponse.from(projects);
  }

  @Transactional(readOnly = true)
  public ProjectResponse getPublicProject(String projectId, Long userId) {
    validateAuthenticatedUser(userId);

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

  private void validateAuthenticatedUser(Long userId) {
    if (!userRepository.existsById(userId)) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
  }
}
