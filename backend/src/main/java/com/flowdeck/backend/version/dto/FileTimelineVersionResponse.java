package com.flowdeck.backend.version.dto;

import java.time.Instant;

public record FileTimelineVersionResponse(
    Long versionId,
    int versionNumber,
    String changeMessage,
    Long createdBy,
    String createdByName,
    Instant createdAt) {

  public static FileTimelineVersionResponse from(
      FileTimelineVersionProjection version, String createdByName) {
    return new FileTimelineVersionResponse(
        version.getId(),
        version.getVersionNumber(),
        version.getChangeMessage(),
        version.getUserId(),
        createdByName,
        version.getCreatedAt());
  }
}
