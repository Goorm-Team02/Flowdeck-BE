package com.flowdeck.backend.integration.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.flowdeck.backend.file.domain.FileType;
import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.file.repository.ProjectFileRepository;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.domain.ProjectVisibility;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.version.domain.FileVersion;
import com.flowdeck.backend.version.dto.FileVersionRestoreResponse;
import com.flowdeck.backend.version.repository.FileVersionRepository;
import com.flowdeck.backend.version.service.FileVersionService;
import com.flowdeck.testsupport.DatabaseIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@DatabaseIntegrationTest
@Transactional
class FileVersionServiceIntegrationTest {

  private final ProjectRepository projectRepository;
  private final ProjectFileRepository projectFileRepository;
  private final FileVersionRepository fileVersionRepository;
  private final FileVersionService fileVersionService;

  @Autowired
  FileVersionServiceIntegrationTest(
      ProjectRepository projectRepository,
      ProjectFileRepository projectFileRepository,
      FileVersionRepository fileVersionRepository,
      FileVersionService fileVersionService) {
    this.projectRepository = projectRepository;
    this.projectFileRepository = projectFileRepository;
    this.fileVersionRepository = fileVersionRepository;
    this.fileVersionService = fileVersionService;
  }

  @Test
  void restoreVersionCreatesNewVersionWithoutChangingPreviousVersions() {
    Project project =
        projectRepository.save(new Project("project", "description", ProjectVisibility.PRIVATE));
    ProjectFile file =
        projectFileRepository.save(new ProjectFile(project, null, "Main.java", FileType.FILE));

    file.increaseVersion();
    FileVersion firstVersion =
        fileVersionRepository.save(new FileVersion(file, null, 1, "v1 content", "first save"));

    file.increaseVersion();
    fileVersionRepository.save(new FileVersion(file, null, 2, "v2 content", "second save"));

    FileVersionRestoreResponse response =
        fileVersionService.restoreVersion(
            project.getPublicId(), file.getId(), firstVersion.getId());

    List<FileVersion> versions = fileVersionRepository.findAllByFileOrderByVersionNumberDesc(file);

    assertThat(response.currentVersion()).isEqualTo(3);
    assertThat(file.getCurrentVersion()).isEqualTo(3);
    assertThat(versions).hasSize(3);
    assertThat(versions).extracting(FileVersion::getVersionNumber).containsExactly(3, 2, 1);
    assertThat(versions.get(0).getContent()).isEqualTo("v1 content");
    assertThat(versions.get(1).getContent()).isEqualTo("v2 content");
    assertThat(versions.get(2).getContent()).isEqualTo("v1 content");
  }
}
