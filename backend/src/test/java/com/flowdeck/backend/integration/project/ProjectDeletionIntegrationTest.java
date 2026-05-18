package com.flowdeck.backend.integration.project;

import static org.assertj.core.api.Assertions.assertThat;

import com.flowdeck.backend.file.domain.FileType;
import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.file.repository.ProjectFileRepository;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.domain.ProjectVisibility;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.backend.project.service.ProjectService;
import com.flowdeck.backend.version.domain.FileVersion;
import com.flowdeck.backend.version.repository.FileVersionRepository;
import com.flowdeck.testsupport.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@DatabaseIntegrationTest
@Transactional
class ProjectDeletionIntegrationTest {

  private final ProjectRepository projectRepository;
  private final ProjectFileRepository projectFileRepository;
  private final FileVersionRepository fileVersionRepository;
  private final ProjectService projectService;

  @Autowired
  ProjectDeletionIntegrationTest(
      ProjectRepository projectRepository,
      ProjectFileRepository projectFileRepository,
      FileVersionRepository fileVersionRepository,
      ProjectService projectService) {
    this.projectRepository = projectRepository;
    this.projectFileRepository = projectFileRepository;
    this.fileVersionRepository = fileVersionRepository;
    this.projectService = projectService;
  }

  @Test
  void deleteProjectAlsoDeletesFilesAndVersions() {
    Project project =
        projectRepository.save(new Project("project", "description", ProjectVisibility.PRIVATE));
    ProjectFile folder =
        projectFileRepository.save(new ProjectFile(project, null, "src", FileType.FOLDER));
    ProjectFile file =
        projectFileRepository.save(new ProjectFile(project, folder, "Main.java", FileType.FILE));
    fileVersionRepository.save(new FileVersion(file, null, 1, "class Main {}", "first save"));

    Long projectId = project.getId();
    Long folderId = folder.getId();
    Long fileId = file.getId();
    String projectPublicId = project.getPublicId();

    projectService.deleteProject(projectPublicId);

    assertThat(projectRepository.findById(projectId)).isEmpty();
    assertThat(projectFileRepository.findById(folderId)).isEmpty();
    assertThat(projectFileRepository.findById(fileId)).isEmpty();
    assertThat(fileVersionRepository.existsByFileId(fileId)).isFalse();
  }
}
