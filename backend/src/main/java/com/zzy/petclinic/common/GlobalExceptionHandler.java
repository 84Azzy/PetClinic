package com.zzy.petclinic.common;

import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> business(BusinessException e) {
    return ResponseEntity.status(e.getStatus())
        .body(ApiResponse.error(e.getStatus().value(), e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException e) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .map(x -> x.getField() + ": " + x.getDefaultMessage())
            .collect(Collectors.joining("; "));
    return ResponseEntity.badRequest().body(ApiResponse.error(400, message));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<Void>> validation(ConstraintViolationException e) {
    return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
  }

  @ExceptionHandler(BadSqlGrammarException.class)
  public ResponseEntity<ApiResponse<Void>> databaseNotReady(BadSqlGrammarException e) {
    log.error("Database query failed", e);
    String message = e.getMostSpecificCause().getMessage();
    if (message != null && message.contains("doesn't exist")) {
      return ResponseEntity.status(503)
          .body(ApiResponse.error(503, "数据库尚未初始化，请先执行 db/schema.sql 与 db/data.sql"));
    }
    return ResponseEntity.internalServerError().body(ApiResponse.error(500, "数据库查询失败"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> unknown(Exception e) {
    log.error("Unhandled request exception", e);
    return ResponseEntity.internalServerError().body(ApiResponse.error(500, "服务器内部错误"));
  }
}
