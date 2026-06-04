package com.flowdeck.backend.projectmessage.realtime;

import com.flowdeck.backend.projectmessage.dto.ProjectMessageResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompProjectMessageBroadcaster implements ProjectMessageBroadcaster {

  private final SimpMessagingTemplate messagingTemplate;

  public StompProjectMessageBroadcaster(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  @Override
  public void broadcastCreated(String projectId, ProjectMessageResponse message) {
    messagingTemplate.convertAndSend(messagesTopic(projectId), createdEvent(message));
  }

  @Override
  public void broadcastDeleted(String projectId, Long messageId) {
    messagingTemplate.convertAndSend(messagesTopic(projectId), deletedEvent(messageId));
  }

  private String messagesTopic(String projectId) {
    return ProjectMessageDestinations.messagesTopic(projectId);
  }
}
