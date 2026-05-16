package com.flowdeck.backend;

import com.flowdeck.backend.support.IsolatedTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** 데이터베이스와 JPA 빈 없이 격리된 테스트 애플리케이션 컨텍스트가 정상 기동하는지 검증한다. */
@ActiveProfiles("test")
@SpringBootTest(classes = IsolatedTestApplication.class)
class IsolatedApplicationContextTest {

  @Test
  void contextLoads() {}
}
