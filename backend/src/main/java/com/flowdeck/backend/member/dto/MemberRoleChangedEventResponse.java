package com.flowdeck.backend.member.dto;

import com.flowdeck.backend.member.domain.ProjectRole;
import java.time.Instant;

public record MemberRoleChangedEventResponse(
    String eventType,
    String projectId,
    Long memberId,
    String userId,
    ProjectRole previousRole,
    ProjectRole currentRole,
    Long actorId,
    String actorName,
    Instant occurredAt) {

  public static MemberRoleChangedEventResponse of(
      String projectId,
      Long memberId,
      String userId,
      ProjectRole previousRole,
      ProjectRole currentRole,
      Long actorId,
      String actorName,
      Instant occurredAt) {
    return new MemberRoleChangedEventResponse(
        "MEMBER_ROLE_CHANGED",
        projectId,
        memberId,
        userId,
        previousRole,
        currentRole,
        actorId,
        actorName,
        occurredAt);
  }
}
