package com.flowdeck.backend.support;

import com.flowdeck.backend.file.repository.FlowFileRepository;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestRepositoryConfig {

  @Bean
  FlowFileRepository flowFileRepository() {
    return Mockito.mock(FlowFileRepository.class);
  }
}
