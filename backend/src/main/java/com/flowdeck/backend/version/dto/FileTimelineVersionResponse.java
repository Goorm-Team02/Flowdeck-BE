package com.flowdeck.backend.version.dto;

import com.flowdeck.backend.version.domain.FileVersion;
import java.time.Instant;

public record FileTimelineVersionResponse(
    Long versionId,
    int versionNumber,
    String changeMessage,
    Long createdBy,
    String createdByName,
    Instant createdAt,
    Integer addedLinesFromPrevious,
    Integer removedLinesFromPrevious) {

  public static FileTimelineVersionResponse from(
      FileVersion version,
      String createdByName,
      Integer addedLinesFromPrevious,
      Integer removedLinesFromPrevious) {
    return new FileTimelineVersionResponse(
        version.getId(),
        version.getVersionNumber(),
        version.getChangeMessage(),
        version.getUserId(),
        createdByName,
        version.getCreatedAt(),
        addedLinesFromPrevious,
        removedLinesFromPrevious);
  }
}
