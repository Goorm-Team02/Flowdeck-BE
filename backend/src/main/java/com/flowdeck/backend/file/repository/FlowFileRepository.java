package com.flowdeck.backend.file.repository;

import com.flowdeck.backend.file.domain.FlowFile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlowFileRepository extends JpaRepository<FlowFile, Long> {

  boolean existsByProjectIdAndParentIdIsNullAndName(Long projectId, String name);

  boolean existsByProjectIdAndParentIdIsNullAndNameAndIdNot(Long projectId, String name, Long id);

  boolean existsByProjectIdAndParentIdAndName(Long projectId, Long parentId, String name);

  boolean existsByProjectIdAndParentIdAndNameAndIdNot(
      Long projectId, Long parentId, String name, Long id);

  Optional<FlowFile> findByIdAndProjectId(Long id, Long projectId);

  List<FlowFile> findAllByProjectIdOrderByParentIdAscNameAsc(Long projectId);
}
