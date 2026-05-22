package com.flowdeck.backend.version.dto;

import com.flowdeck.backend.version.domain.FileVersion;
import java.time.Instant;

public record FileTimelineSelectedVersionResponse(
    Long versionId,
    int versionNumber,
    String changeMessage,
    Long createdBy,
    String createdByName,
    Instant createdAt,
    String content,
    Integer addedLinesFromPrevious,
    Integer removedLinesFromPrevious) {

  public static FileTimelineSelectedVersionResponse from(
      FileVersion version,
      String createdByName,
      Integer addedLinesFromPrevious,
      Integer removedLinesFromPrevious) {
    return new FileTimelineSelectedVersionResponse(
        version.getId(),
        version.getVersionNumber(),
        version.getChangeMessage(),
        version.getUserId(),
        createdByName,
        version.getCreatedAt(),
        version.getContent(),
        addedLinesFromPrevious,
        removedLinesFromPrevious);
  }
}
