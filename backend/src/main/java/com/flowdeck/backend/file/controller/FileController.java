package com.flowdeck.backend.file.controller;

import com.flowdeck.backend.file.dto.CreateFileRequest;
import com.flowdeck.backend.file.dto.FileResponse;
import com.flowdeck.backend.file.dto.UpdateFileRequest;
import com.flowdeck.backend.file.service.FileService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class FileController {

  private final FileService fileService;

  public FileController(FileService fileService) {
    this.fileService = fileService;
  }

  @PostMapping("/projects/{projectId}/files")
  public ResponseEntity<FileResponse> createFile(
      @PathVariable Long projectId, @Valid @RequestBody CreateFileRequest request) {
    FileResponse response = fileService.createFile(projectId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/projects/{projectId}/files")
  public List<FileResponse> getFiles(@PathVariable Long projectId) {
    return fileService.getFiles(projectId);
  }

  @GetMapping("/projects/{projectId}/files/{fileId}")
  public FileResponse getFile(@PathVariable Long projectId, @PathVariable Long fileId) {
    return fileService.getFile(projectId, fileId);
  }

  @PatchMapping("/projects/{projectId}/files/{fileId}")
  public FileResponse updateFile(
      @PathVariable Long projectId,
      @PathVariable Long fileId,
      @Valid @RequestBody UpdateFileRequest request) {
    return fileService.updateFile(projectId, fileId, request);
  }
}
