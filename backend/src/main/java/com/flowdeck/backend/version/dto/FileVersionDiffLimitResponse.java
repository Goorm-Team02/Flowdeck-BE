package com.flowdeck.backend.version.dto;

public record FileVersionDiffLimitResponse(
    int fromVersion,
    int toVersion,
    int maxLines,
    int maxCharacters,
    int fromLineCount,
    int toLineCount,
    int fromCharacterCount,
    int toCharacterCount) {

  public static FileVersionDiffLimitResponse of(
      int fromVersion,
      int toVersion,
      int maxLines,
      int maxCharacters,
      String fromContent,
      String toContent) {
    return new FileVersionDiffLimitResponse(
        fromVersion,
        toVersion,
        maxLines,
        maxCharacters,
        countLines(fromContent),
        countLines(toContent),
        fromContent.length(),
        toContent.length());
  }

  private static int countLines(String content) {
    return content.split("\\R", -1).length;
  }
}
