package com.flowdeck.backend.global.response;

import com.flowdeck.backend.global.error.ErrorCode;

public class ApiResponse<T> {

  private static final String SUCCESS_CODE = "SUCCESS";
  private static final String SUCCESS_MESSAGE = "요청이 성공했습니다.";

  private final boolean success;
  private final String code;
  private final String message;
  private final T data;

  private ApiResponse(boolean success, String code, String message, T data) {
    this.success = success;
    this.code = code;
    this.message = message;
    this.data = data;
  }

  public static ApiResponse<Void> success() {
    return new ApiResponse<>(true, SUCCESS_CODE, SUCCESS_MESSAGE, null);
  }

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(true, SUCCESS_CODE, SUCCESS_MESSAGE, data);
  }

  public static <T> ApiResponse<T> success(String message, T data) {
    return new ApiResponse<>(true, SUCCESS_CODE, message, data);
  }

  public static ApiResponse<Void> failure(ErrorCode errorCode) {
    return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null);
  }

  public static <T> ApiResponse<T> failure(ErrorCode errorCode, T data) {
    return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), data);
  }

  public boolean isSuccess() {
    return success;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public T getData() {
    return data;
  }
}
