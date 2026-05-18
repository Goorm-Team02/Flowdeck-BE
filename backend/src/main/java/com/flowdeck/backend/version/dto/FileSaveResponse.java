package com.flowdeck.backend.version.dto;

import com.flowdeck.backend.file.domain.ProjectFile;
import java.time.Instant;

public record FileSaveResponse(Long fileId, String name, int currentVersion, Instant updatedAt) {

  public static FileSaveResponse from(ProjectFile file) {
    return new FileSaveResponse(
        file.getId(), file.getName(), file.getCurrentVersion(), file.getUpdatedAt());
  }
}
