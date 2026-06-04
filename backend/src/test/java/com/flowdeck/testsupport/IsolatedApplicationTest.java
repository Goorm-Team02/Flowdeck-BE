package com.flowdeck.testsupport;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 데이터베이스나 도메인 빈 없이 공통 웹/보안/설정 동작만 검증할 때 사용한다. 리포지토리나 서비스 빈이 필요하면 대신 {@link
 * DatabaseIntegrationTest}를 사용해야 한다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ActiveProfiles("test")
@SpringBootTest(classes = IsolatedTestApplication.class)
public @interface IsolatedApplicationTest {}
