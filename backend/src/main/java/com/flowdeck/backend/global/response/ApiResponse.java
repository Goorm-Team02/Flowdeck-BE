package com.flowdeck.backend.global.response;

import com.flowdeck.backend.global.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

  private final boolean success;
  private final String code;
  private final String message;
  private final T data;

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, "OK", "요청이 성공했습니다.", data);
  }

  public static <T> ApiResponse<T> ok(String message, T data) {
    return new ApiResponse<>(true, "OK", message, data);
  }

  public static ApiResponse<Void> message(String message) {
    return new ApiResponse<>(true, "OK", message, null);
  }

  public static ApiResponse<Void> error(ErrorCode errorCode) {
    return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null);
  }

  public static <T> ApiResponse<T> error(ErrorCode errorCode, T data) {
    return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), data);
  }
}
