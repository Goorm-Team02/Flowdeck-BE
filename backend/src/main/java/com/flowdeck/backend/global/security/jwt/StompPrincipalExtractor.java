package com.flowdeck.backend.global.security.jwt;

import java.security.Principal;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;

public final class StompPrincipalExtractor {

  private StompPrincipalExtractor() {
    super();
  }

  public static Long extractUserId(Principal principal) {
    if (principal instanceof Authentication authentication
        && authentication.getPrincipal() instanceof JwtAuthentication jwtAuthentication) {
      return jwtAuthentication.getUserId();
    }

    if (principal instanceof JwtAuthentication jwtAuthentication) {
      return jwtAuthentication.getUserId();
    }

    if (principal instanceof AuthenticatedPrincipal authenticatedPrincipal) {
      throw new IllegalStateException(
          "Unsupported WebSocket principal: " + authenticatedPrincipal.getClass().getName());
    }

    throw new IllegalStateException("WebSocket authentication is required.");
  }
}
