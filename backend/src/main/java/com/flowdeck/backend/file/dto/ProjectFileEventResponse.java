package com.flowdeck.backend.file.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.flowdeck.backend.file.domain.ProjectFile;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectFileEventResponse(
    ProjectFileEventType eventType,
    String projectId,
    Long fileId,
    Long actorId,
    String actorName,
    long editRevision,
    int currentVersion,
    Instant occurredAt,
    String oldName,
    String newName,
    Long oldParentId,
    Long newParentId,
    List<Long> deletedFileIds) {

  public static ProjectFileEventResponse created(
      String projectId, ProjectFile file, Long actorId, String actorName) {
    return new ProjectFileEventResponse(
        ProjectFileEventType.FILE_CREATED,
        projectId,
        file.getId(),
        actorId,
        actorName,
        file.getEditRevision(),
        file.getCurrentVersion(),
        Instant.now(),
        null,
        file.getName(),
        null,
        parentId(file),
        null);
  }

  public static ProjectFileEventResponse saved(
      String projectId, ProjectFile file, Long actorId, String actorName) {
    return baseEvent(ProjectFileEventType.FILE_SAVED, projectId, file, actorId, actorName);
  }

  public static ProjectFileEventResponse restored(
      String projectId, ProjectFile file, Long actorId, String actorName) {
    return baseEvent(ProjectFileEventType.FILE_RESTORED, projectId, file, actorId, actorName);
  }

  public static ProjectFileEventResponse deleted(
      String projectId,
      ProjectFile file,
      Long actorId,
      String actorName,
      List<Long> deletedFileIds) {
    return new ProjectFileEventResponse(
        ProjectFileEventType.FILE_DELETED,
        projectId,
        file.getId(),
        actorId,
        actorName,
        file.getEditRevision(),
        file.getCurrentVersion(),
        Instant.now(),
        null,
        null,
        null,
        null,
        List.copyOf(deletedFileIds));
  }

  public static ProjectFileEventResponse renamed(
      String projectId, ProjectFile file, Long actorId, String actorName, String oldName) {
    return new ProjectFileEventResponse(
        ProjectFileEventType.FILE_RENAMED,
        projectId,
        file.getId(),
        actorId,
        actorName,
        file.getEditRevision(),
        file.getCurrentVersion(),
        Instant.now(),
        oldName,
        file.getName(),
        null,
        null,
        null);
  }

  public static ProjectFileEventResponse moved(
      String projectId, ProjectFile file, Long actorId, String actorName, Long oldParentId) {
    return new ProjectFileEventResponse(
        ProjectFileEventType.FILE_MOVED,
        projectId,
        file.getId(),
        actorId,
        actorName,
        file.getEditRevision(),
        file.getCurrentVersion(),
        Instant.now(),
        null,
        null,
        oldParentId,
        parentId(file),
        null);
  }

  private static ProjectFileEventResponse baseEvent(
      ProjectFileEventType eventType,
      String projectId,
      ProjectFile file,
      Long actorId,
      String actorName) {
    return new ProjectFileEventResponse(
        eventType,
        projectId,
        file.getId(),
        actorId,
        actorName,
        file.getEditRevision(),
        file.getCurrentVersion(),
        Instant.now(),
        null,
        null,
        null,
        null,
        null);
  }

  private static Long parentId(ProjectFile file) {
    return file.getParent() == null ? null : file.getParent().getId();
  }
}
