package com.flowdeck.backend.global.security;

import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class CustomAccessDeniedHandler
    implements org.springframework.security.web.access.AccessDeniedHandler {

  private final HandlerExceptionResolver handlerExceptionResolver;

  public CustomAccessDeniedHandler(
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
    this.handlerExceptionResolver = handlerExceptionResolver;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      org.springframework.security.access.AccessDeniedException accessDeniedException)
      throws IOException, ServletException {
    handlerExceptionResolver.resolveException(
        request, response, null, new BusinessException(ErrorCode.ACCESS_DENIED));
  }
}
