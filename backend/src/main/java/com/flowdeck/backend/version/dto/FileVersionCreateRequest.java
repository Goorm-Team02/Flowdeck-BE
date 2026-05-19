package com.flowdeck.backend.version.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FileVersionCreateRequest {

  @NotBlank(message = "변경 메시지는 필수입니다.")
  @Size(max = 255, message = "변경 메시지는 255자 이하여야 합니다.")
  private String changeMessage;

  public String getChangeMessage() {
    return changeMessage;
  }
}
