package com.flowdeck.backend.global.security.jwt;

import com.flowdeck.backend.permission.service.PermissionService;
import com.flowdeck.backend.projectmessage.realtime.ProjectMessageDestinations;
import java.security.Principal;
import java.util.List;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StompJwtChannelInterceptor implements ChannelInterceptor {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenProvider jwtTokenProvider;
  private final PermissionService permissionService;

  public StompJwtChannelInterceptor(
      JwtTokenProvider jwtTokenProvider, PermissionService permissionService) {
    this.jwtTokenProvider = jwtTokenProvider;
    this.permissionService = permissionService;
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null || accessor.getCommand() == null) {
      return message;
    }

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      accessor.setUser(authenticate(accessor));
      return message;
    }

    if (requiresAuthenticatedUser(accessor.getCommand()) && accessor.getUser() == null) {
      throw new IllegalStateException("WebSocket authentication is required.");
    }

    if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
      validateProjectMessageSubscription(accessor);
    }

    return message;
  }

  private Principal authenticate(StompHeaderAccessor accessor) {
    String authorizationHeader = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
    if (!StringUtils.hasText(authorizationHeader)
        || !authorizationHeader.startsWith(BEARER_PREFIX)) {
      throw new IllegalStateException("Missing WebSocket bearer token.");
    }

    String accessToken = authorizationHeader.substring(BEARER_PREFIX.length());
    Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);
    return authentication;
  }

  private boolean requiresAuthenticatedUser(StompCommand command) {
    return List.of(StompCommand.SUBSCRIBE, StompCommand.SEND, StompCommand.UNSUBSCRIBE)
        .contains(command);
  }

  private void validateProjectMessageSubscription(StompHeaderAccessor accessor) {
    String projectId =
        ProjectMessageDestinations.extractProjectIdFromMessagesTopic(accessor.getDestination());
    if (projectId == null) {
      return;
    }

    permissionService.validateProjectAccess(projectId, extractUserId(accessor.getUser()));
  }

  private Long extractUserId(Principal principal) {
    if (principal instanceof Authentication authentication
        && authentication.getPrincipal() instanceof JwtAuthentication jwtAuthentication) {
      return jwtAuthentication.getUserId();
    }

    if (principal instanceof JwtAuthentication jwtAuthentication) {
      return jwtAuthentication.getUserId();
    }

    if (principal instanceof AuthenticatedPrincipal authenticatedPrincipal) {
      throw new IllegalStateException(
          "Unsupported WebSocket principal: " + authenticatedPrincipal.getClass().getName());
    }

    throw new IllegalStateException("WebSocket authentication is required.");
  }
}
