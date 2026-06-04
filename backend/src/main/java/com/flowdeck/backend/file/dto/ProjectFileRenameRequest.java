package com.flowdeck.backend.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProjectFileRenameRequest {

  @NotBlank(message = "파일 또는 폴더 이름은 필수입니다.")
  @Size(max = 255, message = "파일 또는 폴더 이름은 255자 이하여야 합니다.")
  private String name;

  public String getName() {
    return name;
  }
}
