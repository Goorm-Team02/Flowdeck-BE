package com.flowdeck.backend.member.dto;

import java.util.List;

public record MemberListResponse(List<MemberResponse> members) {

  public static MemberListResponse from(List<MemberResponse> members) {
    return new MemberListResponse(members);
  }
}
