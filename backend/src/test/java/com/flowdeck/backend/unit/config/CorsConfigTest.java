package com.flowdeck.backend.unit.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.flowdeck.backend.global.config.CorsConfig;
import com.flowdeck.backend.global.config.CorsProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class CorsConfigTest {

  @Test
  void corsConfigurationAllowsOriginPatternsWithCredentials() {
    CorsProperties properties = new CorsProperties();
    properties.setAllowedOrigins(List.of("https://*.flowdeck.example"));
    properties.setAllowedMethods(List.of("GET"));
    properties.setAllowedHeaders(List.of("*"));
    properties.setExposedHeaders(List.of("Authorization"));
    properties.setAllowCredentials(true);
    properties.setMaxAge(3600);

    CorsConfigurationSource source = new CorsConfig().corsConfigurationSource(properties);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/ping");
    request.addHeader("Origin", "https://app.flowdeck.example");

    CorsConfiguration configuration = source.getCorsConfiguration(request);

    assertThat(configuration).isNotNull();
    assertThat(configuration.checkOrigin("https://app.flowdeck.example"))
        .isEqualTo("https://app.flowdeck.example");
    assertThat(configuration.getAllowCredentials()).isTrue();
  }
}
