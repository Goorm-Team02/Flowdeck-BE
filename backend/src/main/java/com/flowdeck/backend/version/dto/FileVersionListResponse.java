package com.flowdeck.backend.version.dto;

import java.util.List;

public record FileVersionListResponse(List<FileVersionResponse> versions) {

  public static FileVersionListResponse from(List<FileVersionResponse> versions) {
    return new FileVersionListResponse(versions);
  }
}
