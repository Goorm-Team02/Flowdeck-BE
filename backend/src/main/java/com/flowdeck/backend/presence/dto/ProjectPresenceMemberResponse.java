package com.flowdeck.backend.presence.dto;

import java.time.Instant;

public record ProjectPresenceMemberResponse(
    Long userId, String userName, int sessionCount, Instant lastSeenAt) {}
