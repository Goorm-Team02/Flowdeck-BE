package com.flowdeck.backend.version.dto;

import com.flowdeck.backend.file.domain.ProjectFile;
import java.util.List;

public record FileTimelineResponse(
    Long fileId,
    String fileName,
    long totalVersions,
    int page,
    int size,
    boolean hasNext,
    List<FileTimelineVersionResponse> versions) {

  public static FileTimelineResponse from(
      ProjectFile file,
      long totalVersions,
      int page,
      int size,
      boolean hasNext,
      List<FileTimelineVersionResponse> versions) {
    return new FileTimelineResponse(
        file.getId(), file.getName(), totalVersions, page, size, hasNext, versions);
  }
}
