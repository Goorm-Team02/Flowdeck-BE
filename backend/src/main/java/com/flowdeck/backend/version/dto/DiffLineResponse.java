package com.flowdeck.backend.version.dto;

public record DiffLineResponse(
    String type, Integer oldLineNumber, Integer newLineNumber, String content) {}
