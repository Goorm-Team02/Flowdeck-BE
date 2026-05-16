package com.flowdeck.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** 스프링 부트 관례에 따라 메인 애플리케이션 컨텍스트가 정상 기동하는지 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
class BackendApplicationTests {

  @Test
  void contextLoads() {}
}
