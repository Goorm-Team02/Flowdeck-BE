package com.flowdeck.testsupport;

import static org.mockito.Mockito.mock;

import com.flowdeck.backend.auth.service.AuthTokenService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class MockAuthTokenServiceConfig {

  @Bean
  @Primary
  AuthTokenService authTokenService() {
    return mock(AuthTokenService.class);
  }
}
