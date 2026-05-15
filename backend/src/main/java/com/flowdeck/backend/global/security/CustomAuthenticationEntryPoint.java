package com.flowdeck.backend.global.security;

import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final HandlerExceptionResolver handlerExceptionResolver;

  public CustomAuthenticationEntryPoint(
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
    this.handlerExceptionResolver = handlerExceptionResolver;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException, ServletException {
    handlerExceptionResolver.resolveException(
        request, response, null, new BusinessException(ErrorCode.UNAUTHORIZED));
  }
}
