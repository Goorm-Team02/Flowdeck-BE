package com.flowdeck.backend.file.dto;

import com.flowdeck.backend.file.domain.FileType;
import com.flowdeck.backend.file.domain.ProjectFile;
import java.time.Instant;

public record ProjectFileResponse(
    Long fileId,
    Long parentId,
    String name,
    FileType type,
    int currentVersion,
    Instant createdAt,
    Instant updatedAt) {

  public static ProjectFileResponse from(ProjectFile file) {
    Long parentId = file.getParent() == null ? null : file.getParent().getId();

    return new ProjectFileResponse(
        file.getId(),
        parentId,
        file.getName(),
        file.getType(),
        file.getCurrentVersion(),
        file.getCreatedAt(),
        file.getUpdatedAt());
  }
}
