package com.flowdeck.backend.project.dto;

import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.domain.ProjectVisibility;
import java.time.Instant;

public record ProjectResponse(
    String id,
    String title,
    String description,
    ProjectVisibility visibility,
    Instant createdAt,
    Instant updatedAt) {

  public static ProjectResponse from(Project project) {
    return new ProjectResponse(
        project.getPublicId(),
        project.getTitle(),
        project.getDescription(),
        project.getVisibility(),
        project.getCreatedAt(),
        project.getUpdatedAt());
  }
}
