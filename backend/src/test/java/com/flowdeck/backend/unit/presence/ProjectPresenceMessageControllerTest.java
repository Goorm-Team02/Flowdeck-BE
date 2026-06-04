package com.flowdeck.backend.unit.presence;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flowdeck.backend.global.security.jwt.JwtAuthentication;
import com.flowdeck.backend.presence.controller.ProjectPresenceMessageController;
import com.flowdeck.backend.presence.dto.ProjectPresenceResponse;
import com.flowdeck.backend.presence.realtime.ProjectPresenceBroadcaster;
import com.flowdeck.backend.presence.service.ProjectPresenceService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class ProjectPresenceMessageControllerTest {

  private ProjectPresenceService projectPresenceService;
  private ProjectPresenceBroadcaster projectPresenceBroadcaster;
  private ProjectPresenceMessageController controller;

  @BeforeEach
  void setUp() {
    projectPresenceService = org.mockito.Mockito.mock(ProjectPresenceService.class);
    projectPresenceBroadcaster = org.mockito.Mockito.mock(ProjectPresenceBroadcaster.class);
    controller =
        new ProjectPresenceMessageController(projectPresenceService, projectPresenceBroadcaster);
  }

  @Test
  void joinProjectPresenceBroadcastsCurrentSnapshot() {
    UsernamePasswordAuthenticationToken principal =
        new UsernamePasswordAuthenticationToken(
            new JwtAuthentication(7L, "tester@flowdeck.com"), null, List.of());
    ProjectPresenceResponse response =
        ProjectPresenceResponse.empty("project-123", Instant.parse("2026-05-20T12:00:00Z"));
    when(projectPresenceService.join("project-123", 7L, "session-1")).thenReturn(response);

    controller.joinProjectPresence("project-123", principal, "session-1");

    verify(projectPresenceService).join("project-123", 7L, "session-1");
    verify(projectPresenceBroadcaster).broadcast(response);
  }

  @Test
  void heartbeatProjectPresenceBroadcastsCurrentSnapshot() {
    UsernamePasswordAuthenticationToken principal =
        new UsernamePasswordAuthenticationToken(
            new JwtAuthentication(7L, "tester@flowdeck.com"), null, List.of());
    ProjectPresenceResponse response =
        ProjectPresenceResponse.empty("project-123", Instant.parse("2026-05-20T12:00:00Z"));
    when(projectPresenceService.heartbeat("project-123", 7L, "session-1")).thenReturn(response);

    controller.heartbeatProjectPresence("project-123", principal, "session-1");

    verify(projectPresenceService).heartbeat("project-123", 7L, "session-1");
    verify(projectPresenceBroadcaster).broadcast(response);
  }
}
