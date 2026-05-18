package com.flowdeck.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserUpdateRequest {

  @NotBlank(message = "이름은 필수입니다.")
  @Size(min = 2, max = 20, message = "이름은 2자 이상 20자 이하여야 합니다.")
  private String name;

  public String getName() {
    return name;
  }
}
