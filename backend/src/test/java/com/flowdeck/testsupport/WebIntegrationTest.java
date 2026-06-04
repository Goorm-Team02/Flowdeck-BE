package com.flowdeck.testsupport;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

/** 데이터베이스와 JPA 없이 격리된 웹 통합 테스트 컨텍스트를 공통 설정으로 제공하는 메타 애노테이션이다. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@IsolatedApplicationTest
@AutoConfigureMockMvc
public @interface WebIntegrationTest {

  // Marker annotation for isolated web integration tests.
}
