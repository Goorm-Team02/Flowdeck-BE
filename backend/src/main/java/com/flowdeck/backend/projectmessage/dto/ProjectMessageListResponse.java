package com.flowdeck.backend.projectmessage.dto;

import java.util.List;

public record ProjectMessageListResponse(List<ProjectMessageResponse> messages) {

  public static ProjectMessageListResponse from(List<ProjectMessageResponse> messages) {
    return new ProjectMessageListResponse(messages);
  }
}
