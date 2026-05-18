package com.flowdeck.backend.global.security.jwt;

import com.flowdeck.backend.auth.service.AuthTokenService;
import com.flowdeck.backend.global.error.BusinessException;
import com.flowdeck.backend.global.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final AuthTokenService authTokenService;
  private final JwtTokenProvider jwtTokenProvider;
  private final HandlerExceptionResolver handlerExceptionResolver;

  public JwtAuthenticationFilter(
      JwtTokenProvider jwtTokenProvider,
      AuthTokenService authTokenService,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
    this.jwtTokenProvider = jwtTokenProvider;
    this.authTokenService = authTokenService;
    this.handlerExceptionResolver = handlerExceptionResolver;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String accessToken = resolveAccessToken(request);

    if (!StringUtils.hasText(accessToken)) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      if (authTokenService.isBlacklisted(accessToken)) {
        throw new BusinessException(ErrorCode.INVALID_TOKEN);
      }

      Long userId = jwtTokenProvider.getUserId(accessToken);
      if (authTokenService.isForceLogout(userId)) {
        throw new BusinessException(ErrorCode.INVALID_TOKEN);
      }

      SecurityContextHolder.getContext()
          .setAuthentication(jwtTokenProvider.getAuthentication(accessToken));
      filterChain.doFilter(request, response);
    } catch (BusinessException exception) {
      SecurityContextHolder.clearContext();
      handlerExceptionResolver.resolveException(request, response, null, exception);
    }
  }

  private String resolveAccessToken(HttpServletRequest request) {
    String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
    if (!StringUtils.hasText(authorizationHeader)
        || !authorizationHeader.startsWith(BEARER_PREFIX)) {
      return null;
    }

    return authorizationHeader.substring(BEARER_PREFIX.length());
  }
}
