package com.flowdeck.backend.projectmessage.controller;

import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import com.flowdeck.backend.global.response.ApiResponse;
import com.flowdeck.backend.global.security.jwt.JwtAuthentication;
import com.flowdeck.backend.projectmessage.dto.ProjectMessageCreateRequest;
import com.flowdeck.backend.projectmessage.dto.ProjectMessageListResponse;
import com.flowdeck.backend.projectmessage.dto.ProjectMessageResponse;
import com.flowdeck.backend.projectmessage.service.ProjectMessageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Tag(name = "Project Messages", description = "프로젝트 메시지 조회, 생성, 검색, 삭제 API")
@RequestMapping("/api/projects/{projectId}/messages")
public class ProjectMessageController {

  private final ProjectMessageService projectMessageService;

  public ProjectMessageController(ProjectMessageService projectMessageService) {
    this.projectMessageService = projectMessageService;
  }

  @GetMapping
  public ApiResponse<ProjectMessageListResponse> getMessages(
      @PathVariable String projectId, @AuthenticationPrincipal JwtAuthentication principal) {
    return ApiResponse.success(
        ProjectMessageListResponse.from(
            projectMessageService.getMessages(projectId, requireUserId(principal))));
  }

  @PostMapping
  public ApiResponse<ProjectMessageResponse> createMessage(
      @PathVariable String projectId,
      @AuthenticationPrincipal JwtAuthentication principal,
      @Valid @RequestBody ProjectMessageCreateRequest request) {
    return ApiResponse.success(
        "메시지가 저장되었습니다.",
        projectMessageService.createMessage(projectId, requireUserId(principal), request));
  }

  @GetMapping("/search")
  public ApiResponse<ProjectMessageListResponse> searchMessages(
      @PathVariable String projectId,
      @AuthenticationPrincipal JwtAuthentication principal,
      @RequestParam @NotBlank(message = "검색어는 필수입니다.") String keyword) {
    return ApiResponse.success(
        ProjectMessageListResponse.from(
            projectMessageService.searchMessages(projectId, requireUserId(principal), keyword)));
  }

  @DeleteMapping("/{messageId}")
  public ApiResponse<Void> deleteMessage(
      @PathVariable String projectId,
      @PathVariable Long messageId,
      @AuthenticationPrincipal JwtAuthentication principal) {
    projectMessageService.deleteMessage(projectId, messageId, requireUserId(principal));
    return ApiResponse.success("메시지가 삭제되었습니다.", null);
  }

  private Long requireUserId(JwtAuthentication principal) {
    if (principal == null || principal.getUserId() == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    return principal.getUserId();
  }
}
