package com.flowdeck.backend.file.dto;

import com.flowdeck.backend.file.domain.FileType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProjectFileCreateRequest {

  private Long parentId;

  @NotBlank(message = "파일 또는 폴더 이름은 필수입니다.")
  @Size(max = 255, message = "파일 또는 폴더 이름은 255자 이하여야 합니다.")
  private String name;

  @NotNull(message = "파일 타입은 필수입니다.")
  private FileType type;

  public Long getParentId() {
    return parentId;
  }

  public String getName() {
    return name;
  }

  public FileType getType() {
    return type;
  }
}
