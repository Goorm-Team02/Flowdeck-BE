package com.flowdeck.backend.file.domain;

import com.flowdeck.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "project_files",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_project_files_project_parent_name",
          columnNames = {"project_id", "parent_id", "name"})
    },
    indexes = {
      @Index(name = "idx_project_files_project_parent", columnList = "project_id,parent_id"),
      @Index(name = "idx_project_files_project_created_at", columnList = "project_id,created_at")
    })
public class FlowFile extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "project_id", nullable = false)
  private Long projectId;

  @Column(name = "parent_id")
  private Long parentId;

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 20)
  private FileType type;

  @Column(name = "current_version", nullable = false)
  private int currentVersion;

  protected FlowFile() {
    super();
  }

  private FlowFile(Long projectId, Long parentId, String name, FileType type) {
    this.projectId = projectId;
    this.parentId = parentId;
    this.name = name;
    this.type = type;
    this.currentVersion = 0;
  }

  public static FlowFile create(Long projectId, Long parentId, String name, FileType type) {
    return new FlowFile(projectId, parentId, name, type);
  }

  public void rename(String nextName) {
    this.name = nextName;
  }

  public void moveTo(Long nextParentId) {
    this.parentId = nextParentId;
  }

  public int issueNextVersionNumber() {
    this.currentVersion += 1;
    return this.currentVersion;
  }

  public Long getId() {
    return id;
  }

  public Long getProjectId() {
    return projectId;
  }

  public Long getParentId() {
    return parentId;
  }

  public String getName() {
    return name;
  }

  public FileType getType() {
    return type;
  }

  public int getCurrentVersion() {
    return currentVersion;
  }
}
