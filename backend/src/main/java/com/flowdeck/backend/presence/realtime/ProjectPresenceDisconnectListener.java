package com.flowdeck.backend.presence.realtime;

import com.flowdeck.backend.presence.service.ProjectPresenceService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class ProjectPresenceDisconnectListener {

  private final ProjectPresenceService projectPresenceService;
  private final ProjectPresenceBroadcaster projectPresenceBroadcaster;

  public ProjectPresenceDisconnectListener(
      ProjectPresenceService projectPresenceService,
      ProjectPresenceBroadcaster projectPresenceBroadcaster) {
    this.projectPresenceService = projectPresenceService;
    this.projectPresenceBroadcaster = projectPresenceBroadcaster;
  }

  @EventListener
  public void handleSessionDisconnect(SessionDisconnectEvent event) {
    projectPresenceService
        .leave(event.getSessionId())
        .ifPresent(projectPresenceBroadcaster::broadcast);
  }
}
