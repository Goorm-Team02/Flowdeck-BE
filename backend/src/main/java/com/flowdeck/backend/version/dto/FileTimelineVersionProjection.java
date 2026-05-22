package com.flowdeck.backend.version.dto;

import java.time.Instant;

public interface FileTimelineVersionProjection {

  Long getId();

  int getVersionNumber();

  String getChangeMessage();

  Long getUserId();

  Instant getCreatedAt();
}
