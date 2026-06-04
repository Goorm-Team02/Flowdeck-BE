package com.flowdeck.backend.version.dto;

import com.flowdeck.backend.version.domain.FileVersion;
import java.time.Instant;

public record FileVersionResponse(
    Long versionId, int versionNumber, String changeMessage, Long createdBy, Instant createdAt) {

  public static FileVersionResponse from(FileVersion version) {
    return new FileVersionResponse(
        version.getId(),
        version.getVersionNumber(),
        version.getChangeMessage(),
        version.getUserId(),
        version.getCreatedAt());
  }
}
