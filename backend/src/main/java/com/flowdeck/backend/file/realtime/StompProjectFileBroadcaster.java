package com.flowdeck.backend.file.realtime;

import com.flowdeck.backend.file.dto.ProjectFileEventResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompProjectFileBroadcaster implements ProjectFileBroadcaster {

  private final SimpMessagingTemplate messagingTemplate;

  public StompProjectFileBroadcaster(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  @Override
  public void broadcast(ProjectFileEventResponse event) {
    messagingTemplate.convertAndSend(ProjectFileDestinations.filesTopic(event.projectId()), event);
  }
}
