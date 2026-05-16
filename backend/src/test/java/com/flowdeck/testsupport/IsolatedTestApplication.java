package com.flowdeck.testsupport;

import com.flowdeck.backend.global.config.CorsConfig;
import com.flowdeck.backend.global.config.CorsProperties;
import com.flowdeck.backend.global.config.SwaggerConfig;
import com.flowdeck.backend.global.error.GlobalExceptionHandler;
import com.flowdeck.backend.global.security.CustomAccessDeniedHandler;
import com.flowdeck.backend.global.security.CustomAuthenticationEntryPoint;
import com.flowdeck.backend.global.security.SecurityConfig;
import com.flowdeck.backend.global.security.jwt.JwtAuthenticationFilter;
import com.flowdeck.backend.global.security.jwt.JwtConfig;
import com.flowdeck.backend.global.security.jwt.JwtProperties;
import com.flowdeck.backend.global.security.jwt.JwtTokenProvider;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Import;

/** 데이터베이스와 JPA를 제외하고 웹, 보안, 문서화 관련 빈만 로딩하는 격리 테스트 애플리케이션이다. */
@SpringBootConfiguration
@EnableAutoConfiguration(
    excludeName = {
      "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
      "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration",
      "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
      "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
    })
@ConfigurationPropertiesScan(basePackageClasses = {CorsProperties.class, JwtProperties.class})
@Import({
  CorsConfig.class,
  SwaggerConfig.class,
  SecurityConfig.class,
  GlobalExceptionHandler.class,
  JwtConfig.class,
  JwtTokenProvider.class,
  JwtAuthenticationFilter.class,
  CustomAuthenticationEntryPoint.class,
  CustomAccessDeniedHandler.class,
})
public class IsolatedTestApplication {

  protected IsolatedTestApplication() {
    super();
  }
}
