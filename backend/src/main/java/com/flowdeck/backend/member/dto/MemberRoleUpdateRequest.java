package com.flowdeck.backend.member.dto;

import com.flowdeck.backend.member.domain.ProjectRole;
import jakarta.validation.constraints.NotNull;

public class MemberRoleUpdateRequest {

  @NotNull(message = "권한은 필수입니다.")
  private ProjectRole role;

  public ProjectRole getRole() {
    return role;
  }
}
