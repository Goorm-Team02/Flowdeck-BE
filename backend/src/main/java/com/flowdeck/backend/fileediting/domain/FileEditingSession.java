package com.flowdeck.backend.fileediting.domain;

import java.time.Instant;

public record FileEditingSession(
    String projectId,
    Long fileId,
    Long userId,
    String userName,
    String sessionId,
    Instant lastSeenAt) {}
