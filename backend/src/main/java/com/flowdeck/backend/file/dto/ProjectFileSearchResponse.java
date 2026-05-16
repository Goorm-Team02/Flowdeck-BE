package com.flowdeck.backend.file.dto;

import com.flowdeck.backend.file.domain.FileType;
import com.flowdeck.backend.file.domain.ProjectFile;

public record ProjectFileSearchResponse(
    Long fileId, Long parentId, String name, FileType type, int currentVersion) {

  public static ProjectFileSearchResponse from(ProjectFile file) {
    Long parentId = file.getParent() == null ? null : file.getParent().getId();

    return new ProjectFileSearchResponse(
        file.getId(), parentId, file.getName(), file.getType(), file.getCurrentVersion());
  }
}
