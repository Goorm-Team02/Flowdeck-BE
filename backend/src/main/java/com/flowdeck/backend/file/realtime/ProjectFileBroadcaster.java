package com.flowdeck.backend.file.realtime;

import com.flowdeck.backend.file.dto.ProjectFileEventResponse;

public interface ProjectFileBroadcaster {

  void broadcast(ProjectFileEventResponse event);
}
