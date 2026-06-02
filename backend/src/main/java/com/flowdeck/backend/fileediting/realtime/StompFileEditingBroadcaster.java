package com.flowdeck.backend.fileediting.realtime;

import com.flowdeck.backend.fileediting.dto.FileEditingResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompFileEditingBroadcaster implements FileEditingBroadcaster {

  private final SimpMessagingTemplate messagingTemplate;

  public StompFileEditingBroadcaster(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  @Override
  public void broadcast(FileEditingResponse response) {
    messagingTemplate.convertAndSend(
        FileEditingDestinations.editingTopic(response.projectId(), response.fileId()), response);
  }
}
