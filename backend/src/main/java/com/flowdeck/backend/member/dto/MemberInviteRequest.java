package com.flowdeck.backend.member.dto;

import com.flowdeck.backend.member.domain.ProjectRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MemberInviteRequest {

  @NotBlank(message = "이메일은 필수입니다.")
  @Email(message = "이메일 형식이 올바르지 않습니다.")
  private String email;

  @NotNull(message = "권한은 필수입니다.")
  private ProjectRole role;

  public String getEmail() {
    return email;
  }

  public ProjectRole getRole() {
    return role;
  }
}
