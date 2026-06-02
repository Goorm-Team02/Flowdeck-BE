package com.flowdeck.backend.fileediting.controller;

import com.flowdeck.backend.fileediting.realtime.FileEditingBroadcaster;
import com.flowdeck.backend.fileediting.service.FileEditingService;
import com.flowdeck.backend.global.security.jwt.StompPrincipalExtractor;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class FileEditingMessageController {

  private final FileEditingService fileEditingService;
  private final FileEditingBroadcaster fileEditingBroadcaster;

  public FileEditingMessageController(
      FileEditingService fileEditingService, FileEditingBroadcaster fileEditingBroadcaster) {
    this.fileEditingService = fileEditingService;
    this.fileEditingBroadcaster = fileEditingBroadcaster;
  }

  @MessageMapping("/projects/{projectId}/files/{fileId}/editing/start")
  public void startEditing(
      @DestinationVariable String projectId,
      @DestinationVariable Long fileId,
      Principal principal,
      @Header("simpSessionId") String sessionId) {
    fileEditingBroadcaster.broadcast(
        fileEditingService.startEditing(projectId, fileId, extractUserId(principal), sessionId));
  }

  @MessageMapping("/projects/{projectId}/files/{fileId}/editing/heartbeat")
  public void heartbeatEditing(
      @DestinationVariable String projectId,
      @DestinationVariable Long fileId,
      Principal principal,
      @Header("simpSessionId") String sessionId) {
    fileEditingBroadcaster.broadcast(
        fileEditingService.heartbeatEditing(
            projectId, fileId, extractUserId(principal), sessionId));
  }

  @MessageMapping("/projects/{projectId}/files/{fileId}/editing/stop")
  public void stopEditing(
      @DestinationVariable String projectId,
      @DestinationVariable Long fileId,
      Principal principal,
      @Header("simpSessionId") String sessionId) {
    fileEditingBroadcaster.broadcast(
        fileEditingService.stopEditing(projectId, fileId, extractUserId(principal), sessionId));
  }

  private Long extractUserId(Principal principal) {
    return StompPrincipalExtractor.extractUserId(principal);
  }
}
