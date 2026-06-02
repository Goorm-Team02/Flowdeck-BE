package com.flowdeck.backend.member.realtime;

import com.flowdeck.backend.member.dto.MemberRoleChangedEventResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompProjectMemberBroadcaster implements ProjectMemberBroadcaster {

  private final SimpMessagingTemplate messagingTemplate;

  public StompProjectMemberBroadcaster(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  @Override
  public void broadcastRoleChanged(String targetUserName, MemberRoleChangedEventResponse event) {
    messagingTemplate.convertAndSendToUser(
        targetUserName, ProjectMemberDestinations.memberEventsUserQueue(), event);
  }
}
