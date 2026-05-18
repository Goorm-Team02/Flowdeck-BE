package com.flowdeck.backend.auth.dto;

import com.flowdeck.backend.user.domain.User;

public record SignupResponse(String id, String email, String name) {

  public static SignupResponse from(User user) {
    return new SignupResponse(user.getPublicId(), user.getEmail(), user.getName());
  }
}
