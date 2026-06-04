package com.flowdeck.backend.member.repository;

import com.flowdeck.backend.member.domain.ProjectMember;
import com.flowdeck.backend.member.domain.ProjectRole;
import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.user.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

  List<ProjectMember> findAllByProjectOrderByJoinedAtAsc(Project project);

  Optional<ProjectMember> findByProjectAndUser(Project project, User user);

  Optional<ProjectMember> findByProjectPublicIdAndUser(String projectId, User user);

  Optional<ProjectMember> findByIdAndProject(Long id, Project project);

  List<ProjectMember> findAllByUserAndRole(User user, ProjectRole role);

  List<ProjectMember> findAllByUser(User user);

  @Query(
      """
      select projectMember
      from ProjectMember projectMember
      join fetch projectMember.project project
      where projectMember.user.id = :userId
      order by project.createdAt desc, project.id desc
      """)
  List<ProjectMember> findAllWithProjectByUserIdOrderByProjectCreatedAtDesc(
      @Param("userId") Long userId);

  boolean existsByProjectAndUser(Project project, User user);

  long countByProjectAndRole(Project project, ProjectRole role);

  void deleteAllByProject(Project project);
}
