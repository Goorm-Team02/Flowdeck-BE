package com.flowdeck.backend.member.realtime;

public final class ProjectMemberDestinations {

  private ProjectMemberDestinations() {
    super();
  }

  public static String memberEventsUserQueue() {
    return "/queue/project-members";
  }
}
