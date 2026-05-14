package com.flowdeck.backend.global.exception;

import java.util.List;
import org.springframework.validation.BindingResult;

public record ValidationError(String field, String rejectedValue, String reason) {

  public static List<ValidationError> from(BindingResult bindingResult) {
    return bindingResult.getFieldErrors().stream()
        .map(
            error ->
                new ValidationError(
                    error.getField(),
                    error.getRejectedValue() == null ? "" : error.getRejectedValue().toString(),
                    error.getDefaultMessage()))
        .toList();
  }
}
