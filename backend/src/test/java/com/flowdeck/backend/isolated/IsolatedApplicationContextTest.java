package com.flowdeck.backend.isolated;

import static org.junit.jupiter.api.Assertions.assertNull;

import com.flowdeck.backend.project.controller.ProjectController;
import com.flowdeck.backend.project.repository.ProjectRepository;
import com.flowdeck.testsupport.IsolatedApplicationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/** 데이터베이스와 JPA 빈 없이 격리된 테스트 애플리케이션 컨텍스트가 정상 기동하는지 검증한다. */
@IsolatedApplicationTest
class IsolatedApplicationContextTest {

  private final ApplicationContext applicationContext;

  @Autowired
  IsolatedApplicationContextTest(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Test
  void contextLoads() {
    assertNull(applicationContext.getBeanProvider(ProjectController.class).getIfAvailable());
    assertNull(applicationContext.getBeanProvider(ProjectRepository.class).getIfAvailable());
  }
}
