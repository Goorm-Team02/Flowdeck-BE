package com.flowdeck.backend.projectmessage.dto;

import com.flowdeck.backend.projectmessage.domain.ProjectMessage;
import com.flowdeck.backend.projectmessage.domain.ProjectMessageType;
import java.time.Instant;

public record ProjectMessageResponse(
    Long id,
    Long userId,
    String senderName,
    ProjectMessageType messageType,
    String content,
    Instant createdAt) {

  public static ProjectMessageResponse from(ProjectMessage message, String senderName) {
    return new ProjectMessageResponse(
        message.getId(),
        message.getUserId(),
        senderName,
        message.getMessageType(),
        message.getContent(),
        message.getCreatedAt());
  }
}
