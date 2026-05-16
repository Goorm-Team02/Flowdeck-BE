package com.flowdeck.backend.version.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class FileSaveRequest {

  @NotNull(message = "파일 내용은 필수입니다.")
  private String content;

  @Size(max = 255, message = "변경 메시지는 255자 이하여야 합니다.")
  private String changeMessage;

  public String getContent() {
    return content;
  }

  public String getChangeMessage() {
    return changeMessage;
  }
}
