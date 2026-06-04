package com.flowdeck.backend.fileediting.dto;

import java.time.Instant;

public record FileEditingResponse(
    String projectId,
    Long fileId,
    boolean editing,
    Long editorId,
    String editorName,
    String editorSessionId,
    Instant lastSeenAt,
    Instant occurredAt) {

  public static FileEditingResponse editing(
      String projectId,
      Long fileId,
      Long editorId,
      String editorName,
      String editorSessionId,
      Instant lastSeenAt,
      Instant occurredAt) {
    return new FileEditingResponse(
        projectId, fileId, true, editorId, editorName, editorSessionId, lastSeenAt, occurredAt);
  }

  public static FileEditingResponse empty(String projectId, Long fileId, Instant occurredAt) {
    return new FileEditingResponse(projectId, fileId, false, null, null, null, null, occurredAt);
  }
}
