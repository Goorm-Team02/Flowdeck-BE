package com.flowdeck.backend.file.controller;

import com.flowdeck.backend.file.dto.ProjectFileCreateRequest;
import com.flowdeck.backend.file.dto.ProjectFileMoveRequest;
import com.flowdeck.backend.file.dto.ProjectFileRenameRequest;
import com.flowdeck.backend.file.dto.ProjectFileResponse;
import com.flowdeck.backend.file.dto.ProjectFileTreeResponse;
import com.flowdeck.backend.file.service.ProjectFileService;
import com.flowdeck.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/files")
public class ProjectFileController {

  private final ProjectFileService projectFileService;

  public ProjectFileController(ProjectFileService projectFileService) {
    this.projectFileService = projectFileService;
  }

  @PostMapping
  public ApiResponse<ProjectFileResponse> createFile(
      @PathVariable String projectId, @Valid @RequestBody ProjectFileCreateRequest request) {
    return ApiResponse.success(
        "파일 또는 폴더가 생성되었습니다.", projectFileService.createFile(projectId, request));
  }

  @GetMapping
  public ApiResponse<List<ProjectFileTreeResponse>> getFileTree(@PathVariable String projectId) {
    return ApiResponse.success(projectFileService.getFileTree(projectId));
  }

  @GetMapping("/{fileId}")
  public ApiResponse<ProjectFileResponse> getFile(
      @PathVariable String projectId, @PathVariable Long fileId) {
    return ApiResponse.success(projectFileService.getFile(projectId, fileId));
  }

  @PatchMapping("/{fileId}")
  public ApiResponse<ProjectFileResponse> renameFile(
      @PathVariable String projectId,
      @PathVariable Long fileId,
      @Valid @RequestBody ProjectFileRenameRequest request) {
    return ApiResponse.success(
        "파일 또는 폴더 이름이 변경되었습니다.", projectFileService.renameFile(projectId, fileId, request));
  }

  @PatchMapping("/{fileId}/move")
  public ApiResponse<ProjectFileResponse> moveFile(
      @PathVariable String projectId,
      @PathVariable Long fileId,
      @RequestBody ProjectFileMoveRequest request) {
    return ApiResponse.success(
        "파일 또는 폴더 위치가 변경되었습니다.", projectFileService.moveFile(projectId, fileId, request));
  }

  @DeleteMapping("/{fileId}")
  public ApiResponse<Void> deleteFile(@PathVariable String projectId, @PathVariable Long fileId) {
    projectFileService.deleteFile(projectId, fileId);
    return ApiResponse.success("파일 또는 폴더가 삭제되었습니다.", null);
  }
}
