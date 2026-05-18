package com.flowdeck.backend.projectmessage.dto;

public record ProjectMessageEventResponse(
    ProjectMessageEventType eventType, ProjectMessageResponse message, Long messageId) {

  public static ProjectMessageEventResponse created(ProjectMessageResponse message) {
    return new ProjectMessageEventResponse(ProjectMessageEventType.CREATED, message, message.id());
  }

  public static ProjectMessageEventResponse deleted(Long messageId) {
    return new ProjectMessageEventResponse(ProjectMessageEventType.DELETED, null, messageId);
  }
}
