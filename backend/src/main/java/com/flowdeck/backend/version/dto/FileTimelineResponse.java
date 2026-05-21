package com.flowdeck.backend.version.dto;

import com.flowdeck.backend.file.domain.ProjectFile;
import java.util.List;

public record FileTimelineResponse(
    Long fileId,
    String fileName,
    int totalVersions,
    FileTimelineSelectedVersionResponse selectedVersion,
    FileVersionDiffResponse diffFromPrevious,
    List<FileTimelineVersionResponse> versions) {

  public static FileTimelineResponse empty(ProjectFile file) {
    return new FileTimelineResponse(file.getId(), file.getName(), 0, null, null, List.of());
  }

  public static FileTimelineResponse from(
      ProjectFile file,
      FileTimelineSelectedVersionResponse selectedVersion,
      FileVersionDiffResponse diffFromPrevious,
      List<FileTimelineVersionResponse> versions) {
    return new FileTimelineResponse(
        file.getId(), file.getName(), versions.size(), selectedVersion, diffFromPrevious, versions);
  }
}
