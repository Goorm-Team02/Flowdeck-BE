package com.flowdeck.backend.file.service;

import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.file.repository.ProjectFileRepository;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.version.repository.FileVersionRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProjectFileDeletionService {

  private final ProjectFileRepository projectFileRepository;
  private final FileVersionRepository fileVersionRepository;

  public ProjectFileDeletionService(
      ProjectFileRepository projectFileRepository, FileVersionRepository fileVersionRepository) {
    this.projectFileRepository = projectFileRepository;
    this.fileVersionRepository = fileVersionRepository;
  }

  public void deleteProjectFiles(Project project) {
    List<ProjectFile> files =
        projectFileRepository.findAllByProjectOrderByParentIdAscNameAsc(project);

    for (ProjectFile file : files) {
      if (file.getParent() == null) {
        deleteRecursive(file);
      }
    }
  }

  public void deleteRecursive(ProjectFile file) {
    List<ProjectFile> children = projectFileRepository.findAllByParent(file);
    for (ProjectFile child : children) {
      deleteRecursive(child);
    }

    if (file.isFile()) {
      fileVersionRepository.deleteAllByFileId(file.getId());
    }

    projectFileRepository.delete(file);
  }

  public List<Long> collectDeletedFileIds(ProjectFile file) {
    List<Long> deletedFileIds = new ArrayList<>();
    collectDeletedFileIds(file, deletedFileIds);
    return deletedFileIds;
  }

  private void collectDeletedFileIds(ProjectFile file, List<Long> deletedFileIds) {
    deletedFileIds.add(file.getId());

    List<ProjectFile> children = projectFileRepository.findAllByParent(file);
    for (ProjectFile child : children) {
      collectDeletedFileIds(child, deletedFileIds);
    }
  }
}
