package com.flowdeck.backend.projectmessage.repository;

import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.projectmessage.domain.ProjectMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMessageRepository extends JpaRepository<ProjectMessage, Long> {

  List<ProjectMessage> findByProjectIdOrderByCreatedAtAsc(Long projectId);

  List<ProjectMessage> findByProjectIdAndContentContainingIgnoreCaseOrderByCreatedAtAsc(
      Long projectId, String keyword);

  Optional<ProjectMessage> findByIdAndProjectId(Long id, Long projectId);

  void deleteAllByProject(Project project);
}
