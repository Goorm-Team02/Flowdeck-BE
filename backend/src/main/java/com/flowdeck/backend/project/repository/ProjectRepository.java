package com.flowdeck.backend.project.repository;

import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.project.domain.ProjectVisibility;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

  Optional<Project> findByPublicId(String publicId);

  List<Project> findAllByVisibilityOrderByCreatedAtDesc(ProjectVisibility visibility);
}
