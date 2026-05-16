package com.flowdeck.backend.project.controller;

import com.flowdeck.backend.global.response.ApiResponse;
import com.flowdeck.backend.project.dto.ProjectCreateRequest;
import com.flowdeck.backend.project.dto.ProjectListResponse;
import com.flowdeck.backend.project.dto.ProjectResponse;
import com.flowdeck.backend.project.dto.ProjectUpdateRequest;
import com.flowdeck.backend.project.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

  private final ProjectService projectService;

  public ProjectController(ProjectService projectService) {
    this.projectService = projectService;
  }

  @PostMapping
  public ApiResponse<ProjectResponse> createProject(
      @Valid @RequestBody ProjectCreateRequest request) {
    return ApiResponse.success("프로젝트가 생성되었습니다.", projectService.createProject(request));
  }

  @GetMapping("/{projectId}")
  public ApiResponse<ProjectResponse> getProject(@PathVariable String projectId) {
    return ApiResponse.success(projectService.getProject(projectId));
  }

  @PatchMapping("/{projectId}")
  public ApiResponse<ProjectResponse> updateProject(
      @PathVariable String projectId, @Valid @RequestBody ProjectUpdateRequest request) {
    return ApiResponse.success("프로젝트가 수정되었습니다.", projectService.updateProject(projectId, request));
  }

  @DeleteMapping("/{projectId}")
  public ApiResponse<Void> deleteProject(@PathVariable String projectId) {
    projectService.deleteProject(projectId);
    return ApiResponse.success("프로젝트가 삭제되었습니다.", null);
  }

  @GetMapping("/public")
  public ApiResponse<ProjectListResponse> getPublicProjects() {
    return ApiResponse.success(projectService.getPublicProjects());
  }

  @GetMapping("/public/{projectId}")
  public ApiResponse<ProjectResponse> getPublicProject(@PathVariable String projectId) {
    return ApiResponse.success(projectService.getPublicProject(projectId));
  }
}
