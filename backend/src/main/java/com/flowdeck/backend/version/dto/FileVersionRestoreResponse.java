package com.flowdeck.backend.version.dto;

import com.flowdeck.backend.file.domain.ProjectFile;
import java.time.Instant;

public record FileVersionRestoreResponse(
    Long fileId, String name, int currentVersion, Instant updatedAt) {

  public static FileVersionRestoreResponse from(ProjectFile file) {
    return new FileVersionRestoreResponse(
        file.getId(), file.getName(), file.getCurrentVersion(), file.getUpdatedAt());
  }
}
