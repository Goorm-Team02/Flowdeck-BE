package com.flowdeck.backend.member.controller;

import com.flowdeck.backend.global.response.ApiResponse;
import com.flowdeck.backend.global.security.jwt.JwtAuthentication;
import com.flowdeck.backend.member.dto.MemberInviteRequest;
import com.flowdeck.backend.member.dto.MemberListResponse;
import com.flowdeck.backend.member.dto.MemberResponse;
import com.flowdeck.backend.member.dto.MemberRoleUpdateRequest;
import com.flowdeck.backend.member.service.ProjectMemberService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Project Members", description = "프로젝트 멤버 조회, 초대, 권한 변경, 탈퇴 및 제거 API")
@RequestMapping("/api/projects/{projectId}/members")
public class ProjectMemberController {

  private final ProjectMemberService projectMemberService;

  public ProjectMemberController(ProjectMemberService projectMemberService) {
    this.projectMemberService = projectMemberService;
  }

  @GetMapping
  public ApiResponse<MemberListResponse> getMembers(
      @PathVariable String projectId, @AuthenticationPrincipal JwtAuthentication authentication) {
    return ApiResponse.success(
        projectMemberService.getMembers(projectId, authentication.getUserId()));
  }

  @PostMapping
  public ApiResponse<MemberResponse> inviteMember(
      @PathVariable String projectId,
      @AuthenticationPrincipal JwtAuthentication authentication,
      @Valid @RequestBody MemberInviteRequest request) {
    return ApiResponse.success(
        "멤버가 추가되었습니다.",
        projectMemberService.inviteMember(projectId, authentication.getUserId(), request));
  }

  @PatchMapping("/{memberId}")
  public ApiResponse<MemberResponse> updateMemberRole(
      @PathVariable String projectId,
      @PathVariable Long memberId,
      @AuthenticationPrincipal JwtAuthentication authentication,
      @Valid @RequestBody MemberRoleUpdateRequest request) {
    return ApiResponse.success(
        "멤버 권한이 변경되었습니다.",
        projectMemberService.updateMemberRole(
            projectId, authentication.getUserId(), memberId, request));
  }

  @DeleteMapping("/me")
  public ApiResponse<Void> leaveProject(
      @PathVariable String projectId, @AuthenticationPrincipal JwtAuthentication authentication) {
    projectMemberService.leaveProject(projectId, authentication.getUserId());
    return ApiResponse.success("프로젝트에서 나갔습니다.", null);
  }

  @DeleteMapping("/{memberId}")
  public ApiResponse<Void> removeMember(
      @PathVariable String projectId,
      @PathVariable Long memberId,
      @AuthenticationPrincipal JwtAuthentication authentication) {
    projectMemberService.removeMember(projectId, authentication.getUserId(), memberId);
    return ApiResponse.success("멤버가 제거되었습니다.", null);
  }
}
