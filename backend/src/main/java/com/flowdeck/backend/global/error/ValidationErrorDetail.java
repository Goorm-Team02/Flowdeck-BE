package com.flowdeck.backend.global.error;

public class ValidationErrorDetail {

  private final String field;
  private final String reason;

  public ValidationErrorDetail(String field, String reason) {
    this.field = field;
    this.reason = reason;
  }

  public String getField() {
    return field;
  }

  public String getReason() {
    return reason;
  }
}
