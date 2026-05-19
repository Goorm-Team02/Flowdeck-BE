package com.flowdeck.backend.projectmessage.dto;

import jakarta.validation.constraints.NotBlank;

public record ProjectMessageCreateRequest(@NotBlank(message = "메시지 내용은 필수입니다.") String content) {}
