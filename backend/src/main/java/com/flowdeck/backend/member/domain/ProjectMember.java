package com.flowdeck.backend.member.domain;

import com.flowdeck.backend.project.domain.Project;
import com.flowdeck.backend.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "project_members",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_project_members_project_user",
          columnNames = {"project_id", "user_id"})
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMember {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ProjectRole role;

  @Column(name = "joined_at", nullable = false, updatable = false)
  private Instant joinedAt;

  public ProjectMember(Project project, User user, ProjectRole role) {
    this.project = project;
    this.user = user;
    this.role = role;
    this.joinedAt = Instant.now();
  }

  public void updateRole(ProjectRole role) {
    this.role = role;
  }

  public boolean isOwner() {
    return role == ProjectRole.OWNER;
  }

  public boolean canEdit() {
    return role == ProjectRole.OWNER || role == ProjectRole.EDITOR;
  }
}
