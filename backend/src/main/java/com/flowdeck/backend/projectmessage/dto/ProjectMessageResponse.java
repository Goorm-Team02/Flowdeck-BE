package com.flowdeck.backend.projectmessage.dto;

import com.flowdeck.backend.projectmessage.domain.ProjectMessage;
import com.flowdeck.backend.projectmessage.domain.ProjectMessageType;
import java.time.Instant;

public record ProjectMessageResponse(
    Long id, Long userId, ProjectMessageType messageType, String content, Instant createdAt) {

  public static ProjectMessageResponse from(ProjectMessage message) {
    return new ProjectMessageResponse(
        message.getId(),
        message.getUserId(),
        message.getMessageType(),
        message.getContent(),
        message.getCreatedAt());
  }
}
