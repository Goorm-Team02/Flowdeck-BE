package com.flowdeck.backend.project.dto;

import com.flowdeck.backend.project.domain.ProjectVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProjectCreateRequest {

  @NotBlank(message = "프로젝트 제목은 필수입니다.")
  @Size(max = 100, message = "프로젝트 제목은 100자 이하여야 합니다.")
  private String title;

  private String description;

  @NotNull(message = "프로젝트 공개 여부는 필수입니다.")
  private ProjectVisibility visibility;

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public ProjectVisibility getVisibility() {
    return visibility;
  }
}
