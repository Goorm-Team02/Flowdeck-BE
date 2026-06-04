package com.flowdeck.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class RefreshTokenRequest {

  @NotBlank(message = "Refresh Token은 필수입니다.")
  private String refreshToken;

  public String getRefreshToken() {
    return refreshToken;
  }
}
