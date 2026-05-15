package com.flowdeck.backend.global.security.jwt;

import org.springframework.security.core.AuthenticatedPrincipal;

public class JwtAuthentication implements AuthenticatedPrincipal {

  private final Long userId;
  private final String name;

  public JwtAuthentication(Long userId, String name) {
    this.userId = userId;
    this.name = name;
  }

  public Long getUserId() {
    return userId;
  }

  @Override
  public String getName() {
    return name;
  }
}
