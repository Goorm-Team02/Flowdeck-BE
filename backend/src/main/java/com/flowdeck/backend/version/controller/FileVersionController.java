package com.flowdeck.backend.version.controller;

import com.flowdeck.backend.global.response.ApiResponse;
import com.flowdeck.backend.version.dto.FileVersionDetailResponse;
import com.flowdeck.backend.version.dto.FileVersionDiffResponse;
import com.flowdeck.backend.version.dto.FileVersionListResponse;
import com.flowdeck.backend.version.dto.FileVersionRestoreResponse;
import com.flowdeck.backend.version.service.FileVersionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/files/{fileId}/versions")
public class FileVersionController {

  private final FileVersionService fileVersionService;

  public FileVersionController(FileVersionService fileVersionService) {
    this.fileVersionService = fileVersionService;
  }

  @GetMapping
  public ApiResponse<FileVersionListResponse> getVersions(
      @PathVariable String projectId, @PathVariable Long fileId) {
    return ApiResponse.success(fileVersionService.getVersions(projectId, fileId));
  }

  @GetMapping("/{versionId}")
  public ApiResponse<FileVersionDetailResponse> getVersion(
      @PathVariable String projectId, @PathVariable Long fileId, @PathVariable Long versionId) {
    return ApiResponse.success(fileVersionService.getVersion(projectId, fileId, versionId));
  }

  @PostMapping("/{versionId}/restore")
  public ApiResponse<FileVersionRestoreResponse> restoreVersion(
      @PathVariable String projectId, @PathVariable Long fileId, @PathVariable Long versionId) {
    return ApiResponse.success(
        "파일 버전이 복원되었습니다.", fileVersionService.restoreVersion(projectId, fileId, versionId));
  }

  @GetMapping("/diff")
  public ApiResponse<FileVersionDiffResponse> getDiff(
      @PathVariable String projectId,
      @PathVariable Long fileId,
      @RequestParam int from,
      @RequestParam int to) {
    return ApiResponse.success(fileVersionService.getDiff(projectId, fileId, from, to));
  }
}
