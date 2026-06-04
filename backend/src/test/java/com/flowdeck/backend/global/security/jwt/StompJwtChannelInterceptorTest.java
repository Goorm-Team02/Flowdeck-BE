package com.flowdeck.backend.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.flowdeck.backend.auth.service.AuthTokenService;
import com.flowdeck.backend.permission.service.PermissionService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class StompJwtChannelInterceptorTest {

  private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
  private final AuthTokenService authTokenService = mock(AuthTokenService.class);
  private final PermissionService permissionService = mock(PermissionService.class);

  private StompJwtChannelInterceptor interceptor;

  @BeforeEach
  void setUp() {
    interceptor =
        new StompJwtChannelInterceptor(jwtTokenProvider, authTokenService, permissionService);
  }

  @Test
  void connectWithBlacklistedAccessTokenIsRejected() {
    Message<?> message = connectMessage("blacklisted-token");
    when(authTokenService.isBlacklisted("blacklisted-token")).thenReturn(true);

    assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("access token is invalid");
  }

  @Test
  void connectWithAccessTokenIssuedBeforeForceLogoutIsRejected() {
    Message<?> message = connectMessage("old-token");
    when(jwtTokenProvider.getUserId("old-token")).thenReturn(7L);
    when(jwtTokenProvider.getTokenIssuedAtMillis("old-token")).thenReturn(100L);
    when(authTokenService.isForceLogout(7L, 100L)).thenReturn(true);

    assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("access token is invalid");
  }

  @Test
  void connectWithValidAccessTokenSetsAuthenticatedUser() {
    Message<?> message = connectMessage("valid-token");
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            new JwtAuthentication(7L, "tester@flowdeck.com"), null, List.of());
    when(jwtTokenProvider.getUserId("valid-token")).thenReturn(7L);
    when(jwtTokenProvider.getTokenIssuedAtMillis("valid-token")).thenReturn(200L);
    when(jwtTokenProvider.getAuthentication("valid-token")).thenReturn(authentication);

    interceptor.preSend(message, mock(MessageChannel.class));

    verify(authTokenService).isBlacklisted("valid-token");
    verify(authTokenService).isForceLogout(7L, 200L);
  }

  @Test
  void subscribeToProjectMessagesValidatesProjectAccess() {
    Message<?> message = subscribeMessage("/topic/projects/project-123/messages", 7L);

    interceptor.preSend(message, mock(MessageChannel.class));

    verify(permissionService).validateProjectAccess("project-123", 7L);
  }

  @Test
  void subscribeToProjectPresenceValidatesProjectAccess() {
    Message<?> message = subscribeMessage("/topic/projects/project-123/presence", 7L);

    interceptor.preSend(message, mock(MessageChannel.class));

    verify(permissionService).validateProjectAccess("project-123", 7L);
  }

  @Test
  void subscribeToProjectFilesValidatesProjectAccess() {
    Message<?> message = subscribeMessage("/topic/projects/project-123/files", 7L);

    interceptor.preSend(message, mock(MessageChannel.class));

    verify(permissionService).validateProjectAccess("project-123", 7L);
  }

  @Test
  void subscribeToFileEditingValidatesProjectAccess() {
    Message<?> message = subscribeMessage("/topic/projects/project-123/files/11/editing", 7L);

    interceptor.preSend(message, mock(MessageChannel.class));

    verify(permissionService).validateProjectAccess("project-123", 7L);
  }

  @Test
  void subscribeToNonProjectDestinationSkipsProjectAccessValidation() {
    Message<?> message = subscribeMessage("/topic/system/health", 7L);

    interceptor.preSend(message, mock(MessageChannel.class));

    verifyNoInteractions(permissionService);
  }

  @Test
  void subscribeWithoutAuthenticatedUserIsRejected() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination("/topic/projects/project-123/messages");
    Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

    assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("authentication is required");
  }

  @Test
  void sendToProjectMessagesValidatesEditorPermission() {
    Message<?> message = sendMessage("/app/projects/project-123/messages", 7L);

    interceptor.preSend(message, mock(MessageChannel.class));

    verify(permissionService).validateEditor("project-123", 7L);
  }

  @Test
  void sendToFileEditingValidatesEditorPermission() {
    Message<?> message = sendMessage("/app/projects/project-123/files/11/editing/start", 7L);

    interceptor.preSend(message, mock(MessageChannel.class));

    verify(permissionService).validateEditor("project-123", 7L);
  }

  @Test
  void sendToFileEditingStopValidatesProjectAccess() {
    Message<?> message = sendMessage("/app/projects/project-123/files/11/editing/stop", 7L);

    interceptor.preSend(message, mock(MessageChannel.class));

    verify(permissionService).validateProjectAccess("project-123", 7L);
  }

  @Test
  void sendToNonProjectDestinationSkipsEditorValidation() {
    Message<?> message = sendMessage("/app/system/health", 7L);

    interceptor.preSend(message, mock(MessageChannel.class));

    verifyNoInteractions(permissionService);
  }

  private Message<?> subscribeMessage(String destination, Long userId) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination(destination);
    accessor.setUser(
        new UsernamePasswordAuthenticationToken(
            new JwtAuthentication(userId, "tester@flowdeck.com"), null, List.of()));
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private Message<?> connectMessage(String accessToken) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    accessor.setNativeHeader("Authorization", "Bearer " + accessToken);
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private Message<?> sendMessage(String destination, Long userId) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
    accessor.setDestination(destination);
    accessor.setUser(
        new UsernamePasswordAuthenticationToken(
            new JwtAuthentication(userId, "tester@flowdeck.com"), null, List.of()));
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }
}
