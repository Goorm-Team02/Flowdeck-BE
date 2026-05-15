package com.flowdeck.backend.file.exception;

public class DuplicateFileNameException extends RuntimeException {

  public DuplicateFileNameException(String name) {
    super("file name already exists in this folder: " + name);
  }
}
