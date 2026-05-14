package com.flowdeck.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  // Common
  INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다."),
  ACCESS_DENIED(HttpStatus.FORBIDDEN, "C003", "접근 권한이 없습니다."),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C004", "인증이 필요합니다."),

  // Auth
  TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A001", "토큰이 만료되었습니다."),
  TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 토큰입니다."),
  TOKEN_BLACKLISTED(HttpStatus.UNAUTHORIZED, "A003", "로그아웃된 토큰입니다."),
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A004", "이메일 또는 비밀번호가 올바르지 않습니다."),

  // User
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "존재하지 않는 사용자입니다."),
  EMAIL_DUPLICATED(HttpStatus.CONFLICT, "U002", "이미 사용 중인 이메일입니다."),
  INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "U003", "비밀번호가 올바르지 않습니다."),

  // Project
  PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "존재하지 않는 프로젝트입니다."),
  PROJECT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "P002", "프로젝트 접근 권한이 없습니다."),

  // Member / Permission
  MEMBER_NOT_FOUND(HttpStatus.FORBIDDEN, "M001", "프로젝트 멤버가 아닙니다."),
  PERMISSION_DENIED(HttpStatus.FORBIDDEN, "M002", "권한이 부족합니다."),
  OWNER_CANNOT_LEAVE(HttpStatus.BAD_REQUEST, "M003", "OWNER는 권한 이전 후 나갈 수 있습니다."),
  MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "M004", "이미 프로젝트에 참여 중인 멤버입니다."),

  // File - Backend 2
  FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "F001", "존재하지 않는 파일입니다."),
  FILE_NAME_DUPLICATED(HttpStatus.CONFLICT, "F002", "같은 위치에 동일한 이름이 존재합니다."),

  // Chat - Backend 2
  MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "CH001", "존재하지 않는 메시지입니다."),
  MESSAGE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CH002", "메시지 삭제 권한이 없습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
