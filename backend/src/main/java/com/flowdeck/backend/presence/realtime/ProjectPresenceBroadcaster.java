package com.flowdeck.backend.presence.realtime;

import com.flowdeck.backend.presence.dto.ProjectPresenceResponse;

public interface ProjectPresenceBroadcaster {

  void broadcast(ProjectPresenceResponse response);
}
