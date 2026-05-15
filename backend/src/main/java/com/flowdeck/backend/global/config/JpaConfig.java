package com.flowdeck.backend.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@ConditionalOnProperty(
    name = "app.jpa.auditing.enabled",
    havingValue = "true",
    matchIfMissing = true)
@EnableJpaAuditing
public class JpaConfig {

  protected JpaConfig() {
    super();
  }
}
