package com.flowdeck.backend.projectmessage.controller;

import com.flowdeck.backend.global.security.jwt.StompPrincipalExtractor;
import com.flowdeck.backend.projectmessage.dto.ProjectMessageCreateRequest;
import com.flowdeck.backend.projectmessage.service.ProjectMessageService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
public class ProjectMessageStompController {

  private final ProjectMessageService projectMessageService;

  public ProjectMessageStompController(ProjectMessageService projectMessageService) {
    this.projectMessageService = projectMessageService;
  }

  @MessageMapping("/projects/{projectId}/messages")
  public void createMessage(
      @DestinationVariable String projectId,
      Principal principal,
      @Valid @Payload ProjectMessageCreateRequest request) {
    projectMessageService.createMessage(
        projectId, StompPrincipalExtractor.extractUserId(principal), request);
  }
}
