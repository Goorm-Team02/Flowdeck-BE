package com.flowdeck.backend.global.error;

import java.time.LocalDateTime;

public record ErrorResponse(
    LocalDateTime timestamp, int status, String error, String message, String path) {

  public static ErrorResponse of(
      LocalDateTime timestamp, int status, String error, String message, String path) {
    return new ErrorResponse(timestamp, status, error, message, path);
  }
}
