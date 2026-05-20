package com.flowdeck.backend.version.dto;

import com.flowdeck.backend.file.domain.ProjectFile;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "파일 충돌 응답 상세")
public record FileConflictResponse(
    @Schema(description = "파일 ID", example = "1") Long fileId,
    @Schema(description = "요청자가 기준으로 보낸 수정 번호", example = "3") long baseRevision,
    @Schema(description = "서버에 저장된 현재 수정 번호", example = "5") long currentRevision,
    @Schema(description = "현재 명시적 버전 번호", example = "2") int currentVersion,
    @Schema(description = "서버에 저장된 최신 파일 내용", example = "public class Main {}")
        String latestContent) {

  public static FileConflictResponse from(ProjectFile file, long baseRevision) {
    return new FileConflictResponse(
        file.getId(),
        baseRevision,
        file.getEditRevision(),
        file.getCurrentVersion(),
        file.getCurrentContent());
  }
}
