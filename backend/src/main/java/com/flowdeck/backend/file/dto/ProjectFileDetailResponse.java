package com.flowdeck.backend.file.dto;

import com.flowdeck.backend.file.domain.FileType;
import com.flowdeck.backend.file.domain.ProjectFile;
import java.time.Instant;

public record ProjectFileDetailResponse(
    Long fileId,
    Long parentId,
    String name,
    FileType type,
    int currentVersion,
    long editRevision,
    String content,
    Instant createdAt,
    Instant updatedAt) {

  public static ProjectFileDetailResponse from(ProjectFile file, String content) {
    Long parentId = file.getParent() == null ? null : file.getParent().getId();

    return new ProjectFileDetailResponse(
        file.getId(),
        parentId,
        file.getName(),
        file.getType(),
        file.getCurrentVersion(),
        file.getEditRevision(),
        content,
        file.getCreatedAt(),
        file.getUpdatedAt());
  }
}
