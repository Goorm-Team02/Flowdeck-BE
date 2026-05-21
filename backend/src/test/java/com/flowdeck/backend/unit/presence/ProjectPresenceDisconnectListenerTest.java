package com.flowdeck.backend.unit.presence;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flowdeck.backend.presence.dto.ProjectPresenceResponse;
import com.flowdeck.backend.presence.realtime.ProjectPresenceBroadcaster;
import com.flowdeck.backend.presence.realtime.ProjectPresenceDisconnectListener;
import com.flowdeck.backend.presence.service.ProjectPresenceService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

class ProjectPresenceDisconnectListenerTest {

  private ProjectPresenceService projectPresenceService;
  private ProjectPresenceBroadcaster projectPresenceBroadcaster;
  private ProjectPresenceDisconnectListener listener;

  @BeforeEach
  void setUp() {
    projectPresenceService = org.mockito.Mockito.mock(ProjectPresenceService.class);
    projectPresenceBroadcaster = org.mockito.Mockito.mock(ProjectPresenceBroadcaster.class);
    listener =
        new ProjectPresenceDisconnectListener(projectPresenceService, projectPresenceBroadcaster);
  }

  @Test
  void handleSessionDisconnectBroadcastsUpdatedPresence() {
    ProjectPresenceResponse response =
        ProjectPresenceResponse.empty("project-123", Instant.parse("2026-05-20T12:00:00Z"));
    when(projectPresenceService.leave("session-1")).thenReturn(Optional.of(response));
    SessionDisconnectEvent event =
        new SessionDisconnectEvent(
            this, MessageBuilder.withPayload(new byte[0]).build(), "session-1", CloseStatus.NORMAL);

    listener.handleSessionDisconnect(event);

    verify(projectPresenceService).leave("session-1");
    verify(projectPresenceBroadcaster).broadcast(response);
  }
}
