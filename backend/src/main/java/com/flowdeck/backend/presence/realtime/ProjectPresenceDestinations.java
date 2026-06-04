package com.flowdeck.backend.presence.realtime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProjectPresenceDestinations {

  private static final String PROJECTS_PREFIX = "/projects/";
  private static final String PRESENCE_SUFFIX = "/presence";
  private static final Pattern PRESENCE_TOPIC_PATTERN =
      Pattern.compile("^/topic/projects/([^/]+)/presence$");

  private ProjectPresenceDestinations() {
    super();
  }

  public static String presenceTopic(String projectId) {
    return "/topic" + PROJECTS_PREFIX + projectId + PRESENCE_SUFFIX;
  }

  public static String joinDestination(String projectId) {
    return "/app" + PROJECTS_PREFIX + projectId + PRESENCE_SUFFIX + "/join";
  }

  public static String heartbeatDestination(String projectId) {
    return "/app" + PROJECTS_PREFIX + projectId + PRESENCE_SUFFIX + "/heartbeat";
  }

  public static String extractProjectIdFromPresenceTopic(String destination) {
    if (destination == null) {
      return null;
    }

    Matcher matcher = PRESENCE_TOPIC_PATTERN.matcher(destination);
    if (!matcher.matches()) {
      return null;
    }

    return matcher.group(1);
  }
}
