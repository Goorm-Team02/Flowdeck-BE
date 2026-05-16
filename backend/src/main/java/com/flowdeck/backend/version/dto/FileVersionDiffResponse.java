package com.flowdeck.backend.version.dto;

import java.util.List;

public record FileVersionDiffResponse(
    int fromVersion,
    int toVersion,
    int addedLines,
    int removedLines,
    List<DiffLineResponse> changes) {}
