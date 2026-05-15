package com.flowdeck.backend.file.dto;

import com.flowdeck.backend.file.domain.FileType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFileRequest(
    Long parentId,
    @NotBlank(message = "must not be blank")
        @Size(max = 255, message = "must be at most 255 characters")
        String name,
    @NotNull(message = "must not be null") FileType type) {

  public String normalizedName() {
    if (name == null) {
      return null;
    }
    return name.trim();
  }
}
