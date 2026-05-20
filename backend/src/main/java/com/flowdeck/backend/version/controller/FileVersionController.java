package com.flowdeck.backend.version.controller;

import com.flowdeck.backend.global.response.ApiResponse;
import com.flowdeck.backend.global.security.jwt.JwtAuthentication;
import com.flowdeck.backend.version.dto.FileVersionCreateRequest;
import com.flowdeck.backend.version.dto.FileVersionCreateResponse;
import com.flowdeck.backend.version.dto.FileVersionDetailResponse;
import com.flowdeck.backend.version.dto.FileVersionDiffResponse;
import com.flowdeck.backend.version.dto.FileVersionListResponse;
import com.flowdeck.backend.version.dto.FileVersionRestoreRequest;
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
  @Operation(summary = "파일 버전 목록 조회", description = "파일에 명시적으로 저장된 버전 목록을 최신 버전 순서로 조회합니다.")
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
  @Operation(summary = "파일 버전 상세 조회", description = "특정 파일 버전의 메타데이터와 저장된 코드 내용을 조회합니다.")
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
          "선택한 버전의 내용을 현재 파일 내용으로 복원하고, 복원 이력을 새 버전으로 저장합니다. "
              + "요청의 baseRevision이 현재 editRevision과 같을 때만 복원합니다. "
              + "성공 시 currentVersion과 editRevision이 모두 증가합니다. "
              + "baseRevision이 다르면 FILE_409와 FileConflictResponse를 반환합니다.")
  public ApiResponse<FileVersionRestoreResponse> restoreVersion(
      @PathVariable String projectId,
      @AuthenticationPrincipal JwtAuthentication authentication,
      @PathVariable Long fileId,
      @PathVariable Long versionId,
      @Valid @RequestBody FileVersionRestoreRequest request) {
    return ApiResponse.success(
        "파일 버전이 복원되었습니다.",
        fileVersionService.restoreVersion(
            projectId, authentication.getUserId(), fileId, versionId, request));
  }

  @GetMapping("/diff")
  @Operation(
      summary = "파일 버전 diff 조회",
      description = "두 버전의 저장된 코드 내용을 비교해 추가/삭제/변경 라인 정보를 조회합니다. 코드 타임라인과 버전 비교 화면에서 사용합니다.")
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
