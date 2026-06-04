package com.flowdeck.backend.integration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.flowdeck.backend.project.controller.ProjectController;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.testsupport.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/** H2 기반 전체 애플리케이션 컨텍스트에서 웹 계층과 영속성 계층이 함께 연결되는지 검증한다. */
@DatabaseIntegrationTest
class ApplicationContextTest {

  private final ApplicationContext applicationContext;

  @Autowired
  ApplicationContextTest(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Test
  void loadsApplicationBeans() {
    assertAll(
        () -> assertNotNull(applicationContext.getBean(ProjectController.class)),
        () -> assertNotNull(applicationContext.getBean(ProjectRepository.class)));
  }
}
