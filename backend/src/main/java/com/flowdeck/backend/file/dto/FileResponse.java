package com.flowdeck.backend.file.dto;

import com.flowdeck.backend.file.domain.FileType;
import com.flowdeck.backend.file.domain.FlowFile;
import java.time.LocalDateTime;

public record FileResponse(
    Long fileId,
    Long projectId,
    Long parentId,
    String name,
    FileType type,
    int currentVersion,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static FileResponse from(FlowFile flowFile) {
    return new FileResponse(
        flowFile.getId(),
        flowFile.getProjectId(),
        flowFile.getParentId(),
        flowFile.getName(),
        flowFile.getType(),
        flowFile.getCurrentVersion(),
        flowFile.getCreatedAt(),
        flowFile.getUpdatedAt());
  }
}
