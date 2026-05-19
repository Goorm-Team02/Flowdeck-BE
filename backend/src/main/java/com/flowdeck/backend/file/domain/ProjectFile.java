package com.flowdeck.backend.file.domain;

import com.flowdeck.backend.global.entity.BaseTimeEntity;
import com.flowdeck.backend.project.domain.Project;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "project_files")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectFile extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private ProjectFile parent;

  @Column(nullable = false, length = 255)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private FileType type;

  @Column(name = "current_version", nullable = false)
  private int currentVersion = 0;

  @Column(name = "current_content", nullable = false, columnDefinition = "text")
  private String currentContent = "";

  public ProjectFile(Project project, ProjectFile parent, String name, FileType type) {
    this.project = project;
    this.parent = parent;
    this.name = name;
    this.type = type;
    this.currentVersion = 0;
  }

  public void rename(String name) {
    this.name = name;
  }

  public void move(ProjectFile parent) {
    this.parent = parent;
  }

  public void updateContent(String content) {
    this.currentContent = content;
  }

  public void increaseVersion() {
    this.currentVersion += 1;
  }

  public boolean isFolder() {
    return this.type == FileType.FOLDER;
  }

  public boolean isFile() {
    return this.type == FileType.FILE;
  }
}
