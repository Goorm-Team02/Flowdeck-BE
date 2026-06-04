package com.flowdeck.backend.version.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "파일 버전 저장 요청")
public class FileVersionCreateRequest {

  @Schema(description = "버전 저장 메시지", example = "로그인 기능 구현")
  @NotBlank(message = "변경 메시지는 필수입니다.")
  @Size(max = 255, message = "변경 메시지는 255자 이하여야 합니다.")
  private String changeMessage;

  public String getChangeMessage() {
    return changeMessage;
  }
}
