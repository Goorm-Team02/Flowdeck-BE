package com.flowdeck.testsupport;

import com.flowdeck.backend.BackendApplication;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** H2 기반의 전체 애플리케이션 컨텍스트가 필요한 테스트에 사용한다. 도메인, JPA, 리포지토리 빈이 필요한 테스트는 이 애노테이션을 사용한다. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ActiveProfiles("test")
@SpringBootTest(classes = BackendApplication.class)
public @interface DatabaseIntegrationTest {}
