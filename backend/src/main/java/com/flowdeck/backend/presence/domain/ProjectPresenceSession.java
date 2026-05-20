package com.flowdeck.backend.presence.domain;

import java.time.Instant;

public record ProjectPresenceSession(String projectId, Long userId, Instant lastSeenAt) {}
