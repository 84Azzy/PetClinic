package com.zzy.petclinic.common;

import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiResponse<Void>> authentication(AuthenticationException e) {
    return ResponseEntity.status(401).body(ApiResponse.error(401, "用户名或密码错误"));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> accessDenied(AccessDeniedException e) {
    return ResponseEntity.status(403).body(ApiResponse.error(403, "没有权限执行此操作"));
  }

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

  /**
   * 处理 Servlet multipart 层提前拒绝的大文件。
   *
   * <p>超限请求可能在进入 Controller 和 {@code ImageUploadValidator} 前就失败，因此需要在全局异常层单独转换为
   * 统一的 413 响应。
   */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiResponse<Void>> uploadTooLarge(MaxUploadSizeExceededException e) {
    return ResponseEntity.status(413).body(ApiResponse.error(413, "图片大小不能超过 5MB"));
  }

  /**
   * 将 COS SDK 的客户端错误或服务端错误隐藏为稳定的业务响应。
   *
   * <p>详细异常只写入服务端日志，避免把桶名、请求标识或 SDK 内部信息暴露给浏览器。
   */
  @ExceptionHandler({CosClientException.class, CosServiceException.class})
  public ResponseEntity<ApiResponse<Void>> cosFailure(RuntimeException e) {
    log.error("COS request failed", e);
    return ResponseEntity.status(502).body(ApiResponse.error(502, "图片存储服务暂时不可用"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> unknown(Exception e) {
    log.error("Unhandled request exception", e);
    return ResponseEntity.internalServerError().body(ApiResponse.error(500, "服务器内部错误"));
  }
}
