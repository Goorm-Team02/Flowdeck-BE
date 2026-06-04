package com.flowdeck.backend.member.realtime;

import com.flowdeck.backend.member.dto.MemberRoleChangedEventResponse;

public interface ProjectMemberBroadcaster {

  void broadcastRoleChanged(String targetUserName, MemberRoleChangedEventResponse event);
}
