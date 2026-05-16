package com.flowdeck.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** 테스트 프로필로 메인 BackendApplication 컨텍스트를 직접 기동해 실제 애플리케이션 구성이 정상 연결되는지 검증한다. */
@ActiveProfiles("test")
@SpringBootTest(classes = BackendApplication.class)
class BackendApplicationTests {

  @Test
  void contextLoads() {}
}
