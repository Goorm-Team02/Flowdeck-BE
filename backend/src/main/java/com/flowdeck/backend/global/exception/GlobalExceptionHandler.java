package com.flowdeck.backend.global.exception;

import com.flowdeck.backend.global.response.ApiResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(CustomException.class)
  public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
    ErrorCode errorCode = e.getErrorCode();
    log.warn(
        "[CustomException] code: {}, message: {}", errorCode.getCode(), errorCode.getMessage());

    return ResponseEntity.status(errorCode.getStatus()).body(ApiResponse.error(errorCode));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<List<ValidationError>>> handleValidationException(
      MethodArgumentNotValidException e) {
    BindingResult bindingResult = e.getBindingResult();
    log.warn("[ValidationException] {}", bindingResult.getAllErrors());

    return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
        .body(
            ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE, ValidationError.from(bindingResult)));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
      AuthenticationException e) {
    log.warn("[AuthenticationException] {}", e.getMessage());

    return ResponseEntity.status(ErrorCode.UNAUTHORIZED.getStatus())
        .body(ApiResponse.error(ErrorCode.UNAUTHORIZED));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
    log.warn("[AccessDeniedException] {}", e.getMessage());

    return ResponseEntity.status(ErrorCode.ACCESS_DENIED.getStatus())
        .body(ApiResponse.error(ErrorCode.ACCESS_DENIED));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
    log.error("[UnhandledException] {}", e.getMessage(), e);

    return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
        .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
  }
}
