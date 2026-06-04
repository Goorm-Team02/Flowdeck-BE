package com.flowdeck.backend.file.dto;

import com.flowdeck.backend.file.domain.FileType;
import com.flowdeck.backend.file.domain.ProjectFile;
import java.util.ArrayList;
import java.util.List;

public class ProjectFileTreeResponse {

  private final Long fileId;
  private final Long parentId;
  private final String name;
  private final FileType type;
  private final int currentVersion;
  private final long editRevision;
  private final List<ProjectFileTreeResponse> children = new ArrayList<>();

  private ProjectFileTreeResponse(ProjectFile file) {
    this.fileId = file.getId();
    this.parentId = file.getParent() == null ? null : file.getParent().getId();
    this.name = file.getName();
    this.type = file.getType();
    this.currentVersion = file.getCurrentVersion();
    this.editRevision = file.getEditRevision();
  }

  public static ProjectFileTreeResponse from(ProjectFile file) {
    return new ProjectFileTreeResponse(file);
  }

  public void addChild(ProjectFileTreeResponse child) {
    this.children.add(child);
  }

  public Long getFileId() {
    return fileId;
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

  public long getEditRevision() {
    return editRevision;
  }

  public List<ProjectFileTreeResponse> getChildren() {
    return children;
  }
}
