package com.flowdeck.backend.global.error;

import com.flowdeck.backend.global.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException exception) {
    ErrorCode errorCode = exception.getErrorCode();
    if (exception.getData() == null) {
      return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.failure(errorCode));
    }

    return ResponseEntity.status(errorCode.getHttpStatus())
        .body(ApiResponse.failure(errorCode, exception.getData()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<List<ValidationErrorDetail>>> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception) {
    List<ValidationErrorDetail> validationErrors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(
                fieldError ->
                    new ValidationErrorDetail(
                        fieldError.getField(), fieldError.getDefaultMessage()))
            .toList();

    return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus())
        .body(ApiResponse.failure(ErrorCode.INVALID_INPUT_VALUE, validationErrors));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<List<ValidationErrorDetail>>> handleConstraintViolation(
      ConstraintViolationException exception) {
    List<ValidationErrorDetail> validationErrors =
        exception.getConstraintViolations().stream()
            .map(
                violation ->
                    new ValidationErrorDetail(
                        violation.getPropertyPath().toString(), violation.getMessage()))
            .toList();

    return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus())
        .body(ApiResponse.failure(ErrorCode.INVALID_INPUT_VALUE, validationErrors));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(
      HttpMessageNotReadableException exception) {
    return ResponseEntity.status(ErrorCode.INVALID_REQUEST_BODY.getHttpStatus())
        .body(ApiResponse.failure(ErrorCode.INVALID_REQUEST_BODY));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameter(
      MissingServletRequestParameterException exception) {
    return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus())
        .body(ApiResponse.failure(ErrorCode.INVALID_INPUT_VALUE));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
      AccessDeniedException exception) {
    return ResponseEntity.status(ErrorCode.ACCESS_DENIED.getHttpStatus())
        .body(ApiResponse.failure(ErrorCode.ACCESS_DENIED));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
    return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
        .body(ApiResponse.failure(ErrorCode.INTERNAL_SERVER_ERROR));
  }
}
