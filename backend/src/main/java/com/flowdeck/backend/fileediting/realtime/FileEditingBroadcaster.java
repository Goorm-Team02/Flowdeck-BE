package com.flowdeck.backend.fileediting.realtime;

import com.flowdeck.backend.fileediting.dto.FileEditingResponse;

public interface FileEditingBroadcaster {

  void broadcast(FileEditingResponse response);
}
