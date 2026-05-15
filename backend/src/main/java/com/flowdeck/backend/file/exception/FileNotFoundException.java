package com.flowdeck.backend.file.exception;

public class FileNotFoundException extends RuntimeException {

  public FileNotFoundException(Long fileId) {
    super("file not found: " + fileId);
  }
}
