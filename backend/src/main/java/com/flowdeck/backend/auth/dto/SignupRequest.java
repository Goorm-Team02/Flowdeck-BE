package com.flowdeck.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SignupRequest {

  @NotBlank(message = "이메일은 필수입니다.")
  @Email(message = "이메일 형식이 올바르지 않습니다.")
  private String email;

  @NotBlank(message = "비밀번호는 필수입니다.")
  @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
  private String password;

  @NotBlank(message = "이름은 필수입니다.")
  @Size(min = 2, max = 20, message = "이름은 2자 이상 20자 이하여야 합니다.")
  private String name;

  public String getEmail() {
    return email;
  }

  public String getPassword() {
    return password;
  }

  public String getName() {
    return name;
  }
}
