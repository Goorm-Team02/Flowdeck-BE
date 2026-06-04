package com.flowdeck.backend.file.realtime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProjectFileDestinations {

  private static final String PROJECTS_PREFIX = "/projects/";
  private static final String FILES_SUFFIX = "/files";
  private static final Pattern FILES_TOPIC_PATTERN =
      Pattern.compile("^/topic/projects/([^/]+)/files$");

  private ProjectFileDestinations() {
    super();
  }

  public static String filesTopic(String projectId) {
    return "/topic" + PROJECTS_PREFIX + projectId + FILES_SUFFIX;
  }

  public static String filesApplicationDestination(String projectId) {
    return "/app" + PROJECTS_PREFIX + projectId + FILES_SUFFIX;
  }

  public static String extractProjectIdFromFilesTopic(String destination) {
    if (destination == null) {
      return null;
    }

    Matcher matcher = FILES_TOPIC_PATTERN.matcher(destination);
    if (!matcher.matches()) {
      return null;
    }

    return matcher.group(1);
  }
}
