package com.flowdeck.backend.file.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateFileRequest(
    @Pattern(regexp = ".*\\S.*", message = "must not be blank")
        @Size(max = 255, message = "must be at most 255 characters")
        String name) {

  public boolean hasChanges() {
    return name != null;
  }

  public String normalizedName() {
    if (name == null) {
      return null;
    }
    return name.trim();
  }
}
