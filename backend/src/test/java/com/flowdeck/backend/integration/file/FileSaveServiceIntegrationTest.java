package com.flowdeck.backend.integration.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.flowdeck.backend.file.domain.FileType;
import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.file.repository.ProjectFileRepository;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.domain.ProjectVisibility;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.version.domain.FileVersion;
import com.flowdeck.backend.version.dto.FileSaveRequest;
import com.flowdeck.backend.version.dto.FileSaveResponse;
import com.flowdeck.backend.version.repository.FileVersionRepository;
import com.flowdeck.backend.version.service.FileSaveService;
import com.flowdeck.testsupport.DatabaseIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@DatabaseIntegrationTest
@Transactional
class FileSaveServiceIntegrationTest {

  private final ProjectRepository projectRepository;
  private final ProjectFileRepository projectFileRepository;
  private final FileVersionRepository fileVersionRepository;
  private final FileSaveService fileSaveService;

  @Autowired
  FileSaveServiceIntegrationTest(
      ProjectRepository projectRepository,
      ProjectFileRepository projectFileRepository,
      FileVersionRepository fileVersionRepository,
      FileSaveService fileSaveService) {
    this.projectRepository = projectRepository;
    this.projectFileRepository = projectFileRepository;
    this.fileVersionRepository = fileVersionRepository;
    this.fileSaveService = fileSaveService;
  }

  @Test
  void saveFileCreatesVersionsAndIncreasesCurrentVersion() {
    Project project =
        projectRepository.save(new Project("project", "description", ProjectVisibility.PRIVATE));
    ProjectFile file =
        projectFileRepository.save(new ProjectFile(project, null, "Main.java", FileType.FILE));

    FileSaveResponse firstResponse =
        fileSaveService.saveFile(
            project.getPublicId(), file.getId(), createSaveRequest("class Main {}", "first save"));
    FileSaveResponse secondResponse =
        fileSaveService.saveFile(
            project.getPublicId(),
            file.getId(),
            createSaveRequest("class Main { void run() {} }", "second save"));

    List<FileVersion> versions = fileVersionRepository.findAllByFileOrderByVersionNumberDesc(file);

    assertThat(firstResponse.currentVersion()).isEqualTo(1);
    assertThat(secondResponse.currentVersion()).isEqualTo(2);
    assertThat(file.getCurrentVersion()).isEqualTo(2);
    assertThat(versions).hasSize(2);
    assertThat(versions).extracting(FileVersion::getVersionNumber).containsExactly(2, 1);
    assertThat(versions.get(0).getContent()).isEqualTo("class Main { void run() {} }");
    assertThat(versions.get(1).getContent()).isEqualTo("class Main {}");
  }

  private FileSaveRequest createSaveRequest(String content, String changeMessage) {
    FileSaveRequest request = new FileSaveRequest();
    ReflectionTestUtils.setField(request, "content", content);
    ReflectionTestUtils.setField(request, "changeMessage", changeMessage);
    return request;
  }
}
