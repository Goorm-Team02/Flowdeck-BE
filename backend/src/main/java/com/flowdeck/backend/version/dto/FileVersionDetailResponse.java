package com.flowdeck.backend.version.dto;

import com.flowdeck.backend.version.domain.FileVersion;
import java.time.Instant;

public record FileVersionDetailResponse(
    Long versionId,
    int versionNumber,
    String content,
    String changeMessage,
    Long createdBy,
    Instant createdAt) {

  public static FileVersionDetailResponse from(FileVersion version) {
    return new FileVersionDetailResponse(
        version.getId(),
        version.getVersionNumber(),
        version.getContent(),
        version.getChangeMessage(),
        version.getUserId(),
        version.getCreatedAt());
  }
}
