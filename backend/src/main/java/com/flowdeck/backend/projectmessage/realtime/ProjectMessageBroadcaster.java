package com.flowdeck.backend.projectmessage.realtime;

import com.flowdeck.backend.projectmessage.dto.ProjectMessageEventResponse;
import com.flowdeck.backend.projectmessage.dto.ProjectMessageResponse;

public interface ProjectMessageBroadcaster {

  void broadcastCreated(String projectId, ProjectMessageResponse message);

  void broadcastDeleted(String projectId, Long messageId);

  default ProjectMessageEventResponse createdEvent(ProjectMessageResponse message) {
    return ProjectMessageEventResponse.created(message);
  }

  default ProjectMessageEventResponse deletedEvent(Long messageId) {
    return ProjectMessageEventResponse.deleted(messageId);
  }
}
