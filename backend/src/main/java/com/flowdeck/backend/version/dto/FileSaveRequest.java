package com.flowdeck.backend.version.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "파일 현재 내용 저장 요청")
public class FileSaveRequest {

  @Schema(description = "저장할 파일 현재 내용", example = "public class Main {}")
  @NotNull(message = "파일 내용은 필수입니다.")
  private String content;

  @Schema(description = "파일을 열었을 때 받은 editRevision 값", example = "4")
  @NotNull(message = "기준 수정 번호는 필수입니다.")
  private Long baseRevision;

  @Schema(description = "저장 메시지. 현재 내용 저장에서는 버전 이력을 생성하지 않습니다.", example = "현재 내용 저장")
  @Size(max = 255, message = "변경 메시지는 255자 이하여야 합니다.")
  private String changeMessage;

  public String getContent() {
    return content;
  }

  public Long getBaseRevision() {
    return baseRevision;
  }

  public String getChangeMessage() {
    return changeMessage;
  }
}
