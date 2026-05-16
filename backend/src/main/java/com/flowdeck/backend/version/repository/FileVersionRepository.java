package com.flowdeck.backend.version.repository;

import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.version.domain.FileVersion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {

  Optional<FileVersion> findByFileAndVersionNumber(ProjectFile file, int versionNumber);

  void deleteAllByFile(ProjectFile file);
}
