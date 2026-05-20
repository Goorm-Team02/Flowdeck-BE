package com.flowdeck.backend.version.dto;

import com.flowdeck.backend.file.domain.ProjectFile;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "파일 버전 저장 응답")
public record FileVersionCreateResponse(
    @Schema(description = "파일 ID", example = "1") Long fileId,
    @Schema(description = "파일 이름", example = "Main.java") String name,
    @Schema(description = "명시적으로 저장된 최신 버전 번호", example = "3") int currentVersion,
    @Schema(description = "현재 내용 수정 번호", example = "5") long editRevision,
    @Schema(description = "파일 수정 시각") Instant updatedAt) {

  public static FileVersionCreateResponse from(ProjectFile file) {
    return new FileVersionCreateResponse(
        file.getId(),
        file.getName(),
        file.getCurrentVersion(),
        file.getEditRevision(),
        file.getUpdatedAt());
  }
}
