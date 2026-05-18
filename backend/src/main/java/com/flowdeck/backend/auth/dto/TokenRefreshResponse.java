package com.flowdeck.backend.auth.dto;

public record TokenRefreshResponse(String accessToken, String tokenType) {

  public static TokenRefreshResponse bearer(String accessToken) {
    return new TokenRefreshResponse(accessToken, "Bearer");
  }
}
