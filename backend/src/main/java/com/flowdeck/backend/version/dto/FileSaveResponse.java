package com.flowdeck.backend.version.dto;

import com.flowdeck.backend.file.domain.ProjectFile;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "파일 현재 내용 저장 응답")
public record FileSaveResponse(
    @Schema(description = "파일 ID", example = "1") Long fileId,
    @Schema(description = "파일 이름", example = "Main.java") String name,
    @Schema(description = "명시적으로 저장된 최신 버전 번호", example = "2") int currentVersion,
    @Schema(description = "현재 내용 수정 번호", example = "5") long editRevision,
    @Schema(description = "파일 수정 시각") Instant updatedAt) {

  public static FileSaveResponse from(ProjectFile file) {
    return new FileSaveResponse(
        file.getId(),
        file.getName(),
        file.getCurrentVersion(),
        file.getEditRevision(),
        file.getUpdatedAt());
  }
}
