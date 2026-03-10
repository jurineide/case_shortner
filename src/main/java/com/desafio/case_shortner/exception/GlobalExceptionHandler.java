package com.desafio.case_shortner.exception;

import com.desafio.case_shortner.dataTransferObject.ErrorResponse;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(UrlNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(UrlNotFoundException ex) {
    log.warn("URL not found: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of(404, "NOT_FOUND", ex.getMessage()));
  }

  @ExceptionHandler(UrlExpiredException.class)
  public ResponseEntity<ErrorResponse> handleExpired(UrlExpiredException ex) {
    log.warn("URL expired: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.GONE)
        .body(ErrorResponse.of(410, "URL_EXPIRED", ex.getMessage()));
  }

  @ExceptionHandler(InvalidUrlException.class)
  public ResponseEntity<ErrorResponse> handleInvalidUrl(InvalidUrlException ex) {
    log.warn("Invalid URL: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(ErrorResponse.of(422, "INVALID_URL", ex.getMessage()));
  }

  @ExceptionHandler(DuplicateAliasException.class)
  public ResponseEntity<ErrorResponse> handleDuplicateAlias(DuplicateAliasException ex) {
    log.warn("Duplicate alias: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of(409, "DUPLICATE_ALIAS", ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
    log.warn("Validation error: {}", message);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse.of(400, "VALIDATION_ERROR", message));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
    log.error("Unexpected error", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.of(500, "INTERNAL_ERROR", "An unexpected error occurred"));
  }
}
