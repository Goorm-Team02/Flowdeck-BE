package com.flowdeck.backend.presence.controller;

import com.flowdeck.backend.global.response.ApiResponse;
import com.flowdeck.backend.global.security.jwt.JwtAuthentication;
import com.flowdeck.backend.presence.dto.ProjectPresenceResponse;
import com.flowdeck.backend.presence.service.ProjectPresenceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Project Presence", description = "프로젝트 접속 상태 및 참여자 presence 조회 API")
@RequestMapping("/api/projects/{projectId}/presence")
public class ProjectPresenceController {

  private final ProjectPresenceService projectPresenceService;

  public ProjectPresenceController(ProjectPresenceService projectPresenceService) {
    this.projectPresenceService = projectPresenceService;
  }

  @GetMapping
  public ApiResponse<ProjectPresenceResponse> getPresence(
      @PathVariable String projectId, @AuthenticationPrincipal JwtAuthentication authentication) {
    return ApiResponse.success(
        projectPresenceService.getPresence(projectId, authentication.getUserId()));
  }
}
