package com.flowdeck.backend.user.dto;

import com.flowdeck.backend.user.domain.User;
import java.time.Instant;

public record UserResponse(
    String id, String email, String name, Instant createdAt, Instant updatedAt) {

  public static UserResponse from(User user) {
    return new UserResponse(
        user.getPublicId(),
        user.getEmail(),
        user.getName(),
        user.getCreatedAt(),
        user.getUpdatedAt());
  }
}
