package com.flowdeck.backend.fileediting.realtime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FileEditingDestinations {

  private static final String PROJECTS_PREFIX = "/projects/";
  private static final Pattern EDITING_TOPIC_PATTERN =
      Pattern.compile("^/topic/projects/([^/]+)/files/(\\d+)/editing$");
  private static final Pattern EDITING_APPLICATION_DESTINATION_PATTERN =
      Pattern.compile("^/app/projects/([^/]+)/files/(\\d+)/editing/(start|heartbeat|stop)$");
  private static final Pattern EDITING_STOP_APPLICATION_DESTINATION_PATTERN =
      Pattern.compile("^/app/projects/([^/]+)/files/(\\d+)/editing/stop$");

  private FileEditingDestinations() {
    super();
  }

  public static String editingTopic(String projectId, Long fileId) {
    return "/topic" + PROJECTS_PREFIX + projectId + "/files/" + fileId + "/editing";
  }

  public static String startDestination(String projectId, Long fileId) {
    return "/app" + PROJECTS_PREFIX + projectId + "/files/" + fileId + "/editing/start";
  }

  public static String heartbeatDestination(String projectId, Long fileId) {
    return "/app" + PROJECTS_PREFIX + projectId + "/files/" + fileId + "/editing/heartbeat";
  }

  public static String stopDestination(String projectId, Long fileId) {
    return "/app" + PROJECTS_PREFIX + projectId + "/files/" + fileId + "/editing/stop";
  }

  public static String extractProjectIdFromEditingTopic(String destination) {
    Matcher matcher = match(EDITING_TOPIC_PATTERN, destination);
    return matcher == null ? null : matcher.group(1);
  }

  public static String extractProjectIdFromEditingApplicationDestination(String destination) {
    Matcher matcher = match(EDITING_APPLICATION_DESTINATION_PATTERN, destination);
    return matcher == null ? null : matcher.group(1);
  }

  public static String extractProjectIdFromEditingStopApplicationDestination(String destination) {
    Matcher matcher = match(EDITING_STOP_APPLICATION_DESTINATION_PATTERN, destination);
    return matcher == null ? null : matcher.group(1);
  }

  private static Matcher match(Pattern pattern, String destination) {
    if (destination == null) {
      return null;
    }

    Matcher matcher = pattern.matcher(destination);
    return matcher.matches() ? matcher : null;
  }
}
