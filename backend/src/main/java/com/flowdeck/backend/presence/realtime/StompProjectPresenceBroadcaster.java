package com.flowdeck.backend.presence.realtime;

import com.flowdeck.backend.presence.dto.ProjectPresenceResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompProjectPresenceBroadcaster implements ProjectPresenceBroadcaster {

  private final SimpMessagingTemplate messagingTemplate;

  public StompProjectPresenceBroadcaster(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  @Override
  public void broadcast(ProjectPresenceResponse response) {
    messagingTemplate.convertAndSend(
        ProjectPresenceDestinations.presenceTopic(response.projectId()), response);
  }
}
