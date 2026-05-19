package com.flowdeck.backend.version.repository;

import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.version.domain.FileVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {

  List<FileVersion> findAllByFileOrderByVersionNumberDesc(ProjectFile file);

  Optional<FileVersion> findTopByFileOrderByVersionNumberDesc(ProjectFile file);

  Optional<FileVersion> findByFileAndId(ProjectFile file, Long id);

  Optional<FileVersion> findByFileAndVersionNumber(ProjectFile file, int versionNumber);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from FileVersion version where version.file.id = :fileId")
  void deleteAllByFileId(@Param("fileId") Long fileId);

  boolean existsByFileId(Long fileId);
}
