package com.flowdeck.backend.fileediting.store;

import com.flowdeck.backend.fileediting.domain.FileEditingSession;
import java.util.Optional;

public interface FileEditingStore {

  Optional<FileEditingSession> findSession(String projectId, Long fileId);

  void touchSession(FileEditingSession session);

  void removeSession(String projectId, Long fileId, String sessionId);
}
