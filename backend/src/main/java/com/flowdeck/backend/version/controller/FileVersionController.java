package com.flowdeck.backend.version.controller;

import com.flowdeck.backend.global.response.ApiResponse;
import com.flowdeck.backend.global.security.jwt.JwtAuthentication;
import com.flowdeck.backend.version.dto.FileVersionCreateRequest;
import com.flowdeck.backend.version.dto.FileVersionCreateResponse;
import com.flowdeck.backend.version.dto.FileVersionDetailResponse;
import com.flowdeck.backend.version.dto.FileVersionDiffResponse;
import com.flowdeck.backend.version.dto.FileVersionListResponse;
import com.flowdeck.backend.version.dto.FileVersionRestoreResponse;
import com.flowdeck.backend.version.service.FileVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "File Versions", description = "파일 버전 저장, 조회, 복원, diff 조회 API")
@RequestMapping("/api/projects/{projectId}/files/{fileId}/versions")
public class FileVersionController {

  private final FileVersionService fileVersionService;

  public FileVersionController(FileVersionService fileVersionService) {
    this.fileVersionService = fileVersionService;
  }

  @GetMapping
  public ApiResponse<FileVersionListResponse> getVersions(
      @PathVariable String projectId,
      @AuthenticationPrincipal JwtAuthentication authentication,
      @PathVariable Long fileId) {
    return ApiResponse.success(
        fileVersionService.getVersions(projectId, authentication.getUserId(), fileId));
  }

  @PostMapping
  @Operation(
      summary = "파일 버전 저장",
      description =
          "현재 파일 내용을 버전 스냅샷으로 저장합니다. 저장 성공 시 currentVersion은 증가하지만 editRevision은 증가하지 않습니다.")
  public ApiResponse<FileVersionCreateResponse> createVersion(
      @PathVariable String projectId,
      @AuthenticationPrincipal JwtAuthentication authentication,
      @PathVariable Long fileId,
      @Valid @RequestBody FileVersionCreateRequest request) {
    return ApiResponse.success(
        "파일 버전이 저장되었습니다.",
        fileVersionService.createVersion(projectId, authentication.getUserId(), fileId, request));
  }

  @GetMapping("/{versionId}")
  public ApiResponse<FileVersionDetailResponse> getVersion(
      @PathVariable String projectId,
      @AuthenticationPrincipal JwtAuthentication authentication,
      @PathVariable Long fileId,
      @PathVariable Long versionId) {
    return ApiResponse.success(
        fileVersionService.getVersion(projectId, authentication.getUserId(), fileId, versionId));
  }

  @PostMapping("/{versionId}/restore")
  @Operation(
      summary = "파일 버전 복원",
      description =
          "선택한 버전의 내용을 현재 파일 내용으로 복원하고, 복원 이력을 새 버전으로 저장합니다. 성공 시 currentVersion과 editRevision이 모두 증가합니다.")
  public ApiResponse<FileVersionRestoreResponse> restoreVersion(
      @PathVariable String projectId,
      @AuthenticationPrincipal JwtAuthentication authentication,
      @PathVariable Long fileId,
      @PathVariable Long versionId) {
    return ApiResponse.success(
        "파일 버전이 복원되었습니다.",
        fileVersionService.restoreVersion(
            projectId, authentication.getUserId(), fileId, versionId));
  }

  @GetMapping("/diff")
  public ApiResponse<FileVersionDiffResponse> getDiff(
      @PathVariable String projectId,
      @AuthenticationPrincipal JwtAuthentication authentication,
      @PathVariable Long fileId,
      @RequestParam int from,
      @RequestParam int to) {
    return ApiResponse.success(
        fileVersionService.getDiff(projectId, authentication.getUserId(), fileId, from, to));
  }
}
