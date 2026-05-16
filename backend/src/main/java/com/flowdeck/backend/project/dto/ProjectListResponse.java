package com.flowdeck.backend.project.dto;

import java.util.List;

public record ProjectListResponse(List<ProjectResponse> projects) {

  public static ProjectListResponse from(List<ProjectResponse> projects) {
    return new ProjectListResponse(projects);
  }
}
