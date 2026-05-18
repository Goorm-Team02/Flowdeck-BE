package com.flowdeck.backend.auth.dto;

public record LoginResponse(String accessToken, String refreshToken, String tokenType) {

  public static LoginResponse bearer(String accessToken, String refreshToken) {
    return new LoginResponse(accessToken, refreshToken, "Bearer");
  }
}
