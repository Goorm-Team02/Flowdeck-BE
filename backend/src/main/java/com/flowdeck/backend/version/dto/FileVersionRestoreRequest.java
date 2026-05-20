package com.flowdeck.backend.version.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "파일 버전 복원 요청")
public class FileVersionRestoreRequest {

  @Schema(
      description = "파일을 열었을 때 받은 editRevision 값. 현재 editRevision과 다르면 FILE_409가 반환됩니다.",
      example = "4")
  @NotNull(message = "기준 수정 번호는 필수입니다.")
  private Long baseRevision;

  public Long getBaseRevision() {
    return baseRevision;
  }
}
