package com.flowdeck.backend.unit.projectmessage;

import static org.mockito.Mockito.verify;

import com.flowdeck.backend.global.security.jwt.JwtAuthentication;
import com.flowdeck.backend.projectmessage.controller.ProjectMessageStompController;
import com.flowdeck.backend.projectmessage.dto.ProjectMessageCreateRequest;
import com.flowdeck.backend.projectmessage.service.ProjectMessageService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class ProjectMessageStompControllerTest {

  private ProjectMessageService projectMessageService;
  private ProjectMessageStompController controller;

  @BeforeEach
  void setUp() {
    projectMessageService = org.mockito.Mockito.mock(ProjectMessageService.class);
    controller = new ProjectMessageStompController(projectMessageService);
  }

  @Test
  void createMessageDelegatesToServiceWithAuthenticatedUser() {
    UsernamePasswordAuthenticationToken principal =
        new UsernamePasswordAuthenticationToken(
            new JwtAuthentication(7L, "tester@flowdeck.com"), null, List.of());
    ProjectMessageCreateRequest request = new ProjectMessageCreateRequest("안녕하세요");

    controller.createMessage("project-123", principal, request);

    verify(projectMessageService).createMessage("project-123", 7L, request);
  }
}
