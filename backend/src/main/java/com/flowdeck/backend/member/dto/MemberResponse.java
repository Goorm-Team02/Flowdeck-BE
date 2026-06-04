package com.flowdeck.backend.member.dto;

import com.flowdeck.backend.member.domain.ProjectMember;
import com.flowdeck.backend.member.domain.ProjectRole;
import java.time.Instant;

public record MemberResponse(
    Long memberId, String userId, String email, String name, ProjectRole role, Instant joinedAt) {

  public static MemberResponse from(ProjectMember member) {
    return new MemberResponse(
        member.getId(),
        member.getUser().getPublicId(),
        member.getUser().getEmail(),
        member.getUser().getName(),
        member.getRole(),
        member.getJoinedAt());
  }
}
