package com.flowdeck.backend.presence.dto;

import java.time.Instant;
import java.util.List;

public record ProjectPresenceResponse(
    String projectId,
    int connectedCount,
    List<ProjectPresenceMemberResponse> members,
    Instant occurredAt) {

  public static ProjectPresenceResponse of(
      String projectId, List<ProjectPresenceMemberResponse> members, Instant occurredAt) {
    return new ProjectPresenceResponse(projectId, members.size(), members, occurredAt);
  }

  public static ProjectPresenceResponse empty(String projectId, Instant occurredAt) {
    return new ProjectPresenceResponse(projectId, 0, List.of(), occurredAt);
  }
}
