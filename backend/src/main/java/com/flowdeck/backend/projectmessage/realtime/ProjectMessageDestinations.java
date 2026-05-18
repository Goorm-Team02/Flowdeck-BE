package com.flowdeck.backend.projectmessage.realtime;

public final class ProjectMessageDestinations {

  private static final String PROJECTS_PREFIX = "/projects/";
  private static final String MESSAGES_SUFFIX = "/messages";

  private ProjectMessageDestinations() {
    super();
  }

  public static String messagesTopic(String projectId) {
    return "/topic" + PROJECTS_PREFIX + projectId + MESSAGES_SUFFIX;
  }

  public static String messagesApplicationDestination(String projectId) {
    return "/app" + PROJECTS_PREFIX + projectId + MESSAGES_SUFFIX;
  }
}
