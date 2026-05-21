package com.flowdeck.backend.presence.controller;

import com.flowdeck.backend.global.security.jwt.StompPrincipalExtractor;
import com.flowdeck.backend.presence.realtime.ProjectPresenceBroadcaster;
import com.flowdeck.backend.presence.service.ProjectPresenceService;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ProjectPresenceMessageController {

  private final ProjectPresenceService projectPresenceService;
  private final ProjectPresenceBroadcaster projectPresenceBroadcaster;

  public ProjectPresenceMessageController(
      ProjectPresenceService projectPresenceService,
      ProjectPresenceBroadcaster projectPresenceBroadcaster) {
    this.projectPresenceService = projectPresenceService;
    this.projectPresenceBroadcaster = projectPresenceBroadcaster;
  }

  @MessageMapping("/projects/{projectId}/presence/join")
  public void joinProjectPresence(
      @DestinationVariable String projectId,
      Principal principal,
      @Header("simpSessionId") String sessionId) {
    projectPresenceBroadcaster.broadcast(
        projectPresenceService.join(projectId, extractUserId(principal), sessionId));
  }

  @MessageMapping("/projects/{projectId}/presence/heartbeat")
  public void heartbeatProjectPresence(
      @DestinationVariable String projectId,
      Principal principal,
      @Header("simpSessionId") String sessionId) {
    projectPresenceBroadcaster.broadcast(
        projectPresenceService.heartbeat(projectId, extractUserId(principal), sessionId));
  }

  private Long extractUserId(Principal principal) {
    return StompPrincipalExtractor.extractUserId(principal);
  }
}
