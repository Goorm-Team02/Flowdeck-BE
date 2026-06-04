package com.flowdeck.backend.file.dto;

import com.flowdeck.backend.file.domain.FileType;
import com.flowdeck.backend.file.domain.ProjectFile;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "파일 상세 조회 응답")
public record ProjectFileDetailResponse(
    @Schema(description = "파일 ID", example = "1") Long fileId,
    @Schema(description = "부모 폴더 ID. 루트 파일이면 null입니다.", example = "10") Long parentId,
    @Schema(description = "파일 이름", example = "Main.java") String name,
    @Schema(description = "파일 타입", example = "FILE") FileType type,
    @Schema(description = "명시적으로 저장된 최신 버전 번호", example = "2") int currentVersion,
    @Schema(description = "현재 내용 수정 번호. 다음 저장 요청의 baseRevision으로 사용합니다.", example = "5")
        long editRevision,
    @Schema(description = "에디터에 표시할 현재 파일 내용", example = "public class Main {}") String content,
    @Schema(description = "파일 생성 시각") Instant createdAt,
    @Schema(description = "파일 수정 시각") Instant updatedAt) {

  public static ProjectFileDetailResponse from(ProjectFile file, String content) {
    Long parentId = file.getParent() == null ? null : file.getParent().getId();

    return new ProjectFileDetailResponse(
        file.getId(),
        parentId,
        file.getName(),
        file.getType(),
        file.getCurrentVersion(),
        file.getEditRevision(),
        content,
        file.getCreatedAt(),
        file.getUpdatedAt());
  }
}
