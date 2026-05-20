package com.flowdeck.backend.file.controller;

import com.flowdeck.backend.file.dto.ProjectFileCreateRequest;
import com.flowdeck.backend.file.dto.ProjectFileDetailResponse;
import com.flowdeck.backend.file.dto.ProjectFileMoveRequest;
import com.flowdeck.backend.file.dto.ProjectFileRenameRequest;
import com.flowdeck.backend.file.dto.ProjectFileResponse;
import com.flowdeck.backend.file.dto.ProjectFileSearchResponse;
import com.flowdeck.backend.file.dto.ProjectFileTreeResponse;
import com.flowdeck.backend.file.service.ProjectFileService;
import com.flowdeck.backend.global.response.ApiResponse;
import com.flowdeck.backend.global.security.jwt.JwtAuthentication;
import com.flowdeck.backend.version.dto.FileSaveRequest;
import com.flowdeck.backend.version.dto.FileSaveResponse;
import com.flowdeck.backend.version.service.FileSaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Project Files", description = "프로젝트 파일/폴더 관리 및 현재 파일 내용 저장 API")
@RequestMapping("/api/projects/{projectId}/files")
public class ProjectFileController {

  private final ProjectFileService projectFileService;
  private final FileSaveService fileSaveService;

  public ProjectFileController(
      ProjectFileService projectFileService, FileSaveService fileSaveService) {
    this.projectFileService = projectFileService;
    this.fileSaveService = fileSaveService;
  }

  @PostMapping
  public ApiResponse<ProjectFileResponse> createFile(
      @PathVariable String projectId,
      @AuthenticationPrincipal JwtAuthentication authentication,
      @Valid @RequestBody ProjectFileCreateRequest request) {
    return ApiResponse.success(
        "파일 또는 폴더가 생성되었습니다.",
        projectFileService.createFile(projectId, authentication.getUserId(), request));
  }

  @GetMapping
  public ApiResponse<List<ProjectFileTreeResponse>> getFileTree(
      @PathVariable String projectId, @AuthenticationPrincipal JwtAuthentication authentication) {
    return ApiResponse.success(
        projectFileService.getFileTree(projectId, authentication.getUserId()));
  }

  @GetMapping("/search")
  public ApiResponse<List<ProjectFileSearchResponse>> searchFiles(
      @PathVariable String projectId,
      @AuthenticationPrincipal JwtAuthentication authentication,
      @RequestParam String keyword) {
    return ApiResponse.success(
        projectFileService.searchFiles(projectId, authentication.getUserId(), keyword));
  }

  @GetMapping("/{fileId}")
  @Operation(
      summary = "파일 상세 조회",
      description =
          "파일 메타데이터와 현재 내용을 조회합니다. 응답의 content는 에디터 초기 내용으로, editRevision은 다음 저장 요청의 baseRevision으로 사용합니다.")
  public ApiResponse<ProjectFileDetailResponse> getFile(
      @PathVariable String projectId,
      @AuthenticationPrincipal JwtAuthentication authentication,
      @PathVariable Long fileId) {
    return ApiResponse.success(
        projectFileService.getFile(projectId, authentication.getUserId(), fileId));
  }

  @PatchMapping("/{fileId}")
  public ApiResponse<ProjectFileResponse> renameFile(
      @PathVariable String projectId,
      @AuthenticationPrincipal JwtAuthentication authentication,
      @PathVariable Long fileId,
      @Valid @RequestBody ProjectFileRenameRequest request) {
    return ApiResponse.success(
        "파일 또는 폴더 이름이 변경되었습니다.",
        projectFileService.renameFile(projectId, authentication.getUserId(), fileId, request));
  }

  @PatchMapping("/{fileId}/move")
  public ApiResponse<ProjectFileResponse> moveFile(
      @PathVariable String projectId,
      @AuthenticationPrincipal JwtAuthentication authentication,
      @PathVariable Long fileId,
      @RequestBody ProjectFileMoveRequest request) {
    return ApiResponse.success(
        "파일 또는 폴더 위치가 변경되었습니다.",
        projectFileService.moveFile(projectId, authentication.getUserId(), fileId, request));
  }

  @DeleteMapping("/{fileId}")
  public ApiResponse<Void> deleteFile(
      @PathVariable String projectId,
      @AuthenticationPrincipal JwtAuthentication authentication,
      @PathVariable Long fileId,
      @RequestParam Long expectedRevision) {
    projectFileService.deleteFile(projectId, authentication.getUserId(), fileId, expectedRevision);
    return ApiResponse.success("파일 또는 폴더가 삭제되었습니다.", null);
  }

  @PutMapping("/{fileId}")
  @Operation(
      summary = "파일 현재 내용 저장",
      description =
          "파일의 현재 내용을 저장합니다. 저장 성공 시 editRevision은 증가하지만 currentVersion은 증가하지 않으며 FileVersion도 생성하지 않습니다.")
  public ApiResponse<FileSaveResponse> saveFile(
      @PathVariable String projectId,
      @AuthenticationPrincipal JwtAuthentication authentication,
      @PathVariable Long fileId,
      @Valid @RequestBody FileSaveRequest request) {
    return ApiResponse.success(
        "파일이 저장되었습니다.",
        fileSaveService.saveFile(projectId, authentication.getUserId(), fileId, request));
  }
}
