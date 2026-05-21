package com.flowdeck.backend.projectmessage.realtime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProjectMessageDestinations {

  private static final String PROJECTS_PREFIX = "/projects/";
  private static final String MESSAGES_SUFFIX = "/messages";
  private static final Pattern MESSAGES_TOPIC_PATTERN =
      Pattern.compile("^/topic/projects/([^/]+)/messages$");
  private static final Pattern MESSAGES_APPLICATION_DESTINATION_PATTERN =
      Pattern.compile("^/app/projects/([^/]+)/messages$");

  private ProjectMessageDestinations() {
    super();
  }

  public static String messagesTopic(String projectId) {
    return "/topic" + PROJECTS_PREFIX + projectId + MESSAGES_SUFFIX;
  }

  public static String messagesApplicationDestination(String projectId) {
    return "/app" + PROJECTS_PREFIX + projectId + MESSAGES_SUFFIX;
  }

  public static String extractProjectIdFromMessagesTopic(String destination) {
    if (destination == null) {
      return null;
    }

    Matcher matcher = MESSAGES_TOPIC_PATTERN.matcher(destination);
    if (!matcher.matches()) {
      return null;
    }

    return matcher.group(1);
  }

  public static String extractProjectIdFromMessagesApplicationDestination(String destination) {
    if (destination == null) {
      return null;
    }

    Matcher matcher = MESSAGES_APPLICATION_DESTINATION_PATTERN.matcher(destination);
    if (!matcher.matches()) {
      return null;
    }

    return matcher.group(1);
  }
}
