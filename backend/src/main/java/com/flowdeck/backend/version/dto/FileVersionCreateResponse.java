package com.flowdeck.backend.version.dto;

import com.flowdeck.backend.file.domain.ProjectFile;
import java.time.Instant;

public record FileVersionCreateResponse(
    Long fileId, String name, int currentVersion, Instant updatedAt) {

  public static FileVersionCreateResponse from(ProjectFile file) {
    return new FileVersionCreateResponse(
        file.getId(), file.getName(), file.getCurrentVersion(), file.getUpdatedAt());
  }
}
