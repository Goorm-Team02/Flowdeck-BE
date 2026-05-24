package com.flowdeck.backend.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
  INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청입니다."),
  INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "COMMON_400_1", "요청 본문을 확인해 주세요."),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401", "인증이 필요합니다."),
  INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_1", "유효하지 않은 토큰입니다."),
  ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_403", "접근 권한이 없습니다."),
  USER_WITHDRAWAL_BLOCKED(
      HttpStatus.CONFLICT, "USER_409", "소유권 이전이 필요한 프로젝트가 있어 회원 탈퇴를 진행할 수 없습니다."),
  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_404", "리소스를 찾을 수 없습니다."),
  FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE_404", "파일을 찾을 수 없습니다."),
  FILE_INVALID_TYPE(HttpStatus.BAD_REQUEST, "FILE_400", "파일 타입이 올바르지 않습니다."),
  FILE_INVALID_KEYWORD(HttpStatus.BAD_REQUEST, "FILE_400_1", "검색어가 올바르지 않습니다."),
  FILE_INVALID_MOVE_TARGET(HttpStatus.BAD_REQUEST, "FILE_400_2", "파일을 해당 위치로 이동할 수 없습니다."),
  FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "FILE_400_3", "파일 크기 제한을 초과했습니다."),
  FILE_EDIT_CONFLICT(HttpStatus.CONFLICT, "FILE_409", "파일이 다른 사용자에 의해 수정되었습니다."),
  FILE_NAME_DUPLICATED(HttpStatus.CONFLICT, "FILE_409_1", "같은 위치에 동일한 이름이 존재합니다."),
  VERSION_NOT_FOUND(HttpStatus.NOT_FOUND, "VERSION_404", "파일 버전을 찾을 수 없습니다."),
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 내부 오류가 발생했습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  ErrorCode(HttpStatus httpStatus, String code, String message) {
    this.httpStatus = httpStatus;
    this.code = code;
    this.message = message;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
