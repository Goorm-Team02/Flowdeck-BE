package com.flowdeck.testsupport;

import com.flowdeck.backend.global.security.jwt.JwtTokenProvider;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class JwtBearerTokenTestSupport {

  @Autowired private JwtTokenProvider jwtTokenProvider;

  protected String bearerToken(Long userId) {
    return "Bearer "
        + jwtTokenProvider.createAccessToken(
            userId, "tester" + userId + "@flowdeck.com", List.of("ROLE_USER"));
  }
}
