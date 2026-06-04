package com.flowdeck.backend.version.repository;

import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.version.domain.FileVersion;
import com.flowdeck.backend.version.dto.FileTimelineVersionProjection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {

  List<FileVersion> findAllByFileOrderByVersionNumberAsc(ProjectFile file);

  @Query(
      value =
          """
          select version.id as id,
                 version.versionNumber as versionNumber,
                 version.changeMessage as changeMessage,
                 version.userId as userId,
                 version.createdAt as createdAt
          from FileVersion version
          where version.file = :file
          order by version.versionNumber asc
          """,
      countQuery =
          """
          select count(version)
          from FileVersion version
          where version.file = :file
          """)
  Page<FileTimelineVersionProjection> findTimelineVersionsByFile(
      @Param("file") ProjectFile file, Pageable pageable);

  List<FileVersion> findAllByFileOrderByVersionNumberDesc(ProjectFile file);

  Optional<FileVersion> findByFileAndId(ProjectFile file, Long id);

  Optional<FileVersion> findByFileAndVersionNumber(ProjectFile file, int versionNumber);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from FileVersion version where version.file.id = :fileId")
  void deleteAllByFileId(@Param("fileId") Long fileId);

  boolean existsByFileId(Long fileId);
}
