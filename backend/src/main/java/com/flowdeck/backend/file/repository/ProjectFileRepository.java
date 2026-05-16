package com.flowdeck.backend.file.repository;

import com.flowdeck.backend.file.domain.ProjectFile;
import com.flowdeck.backend.project.domain.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectFileRepository extends JpaRepository<ProjectFile, Long> {

  List<ProjectFile> findAllByProjectOrderByParentIdAscNameAsc(Project project);

  List<ProjectFile> findAllByParent(ProjectFile parent);

  Optional<ProjectFile> findByIdAndProject(Long id, Project project);

  boolean existsByProjectAndParentAndName(Project project, ProjectFile parent, String name);

  boolean existsByProjectAndParentIsNullAndName(Project project, String name);
}
