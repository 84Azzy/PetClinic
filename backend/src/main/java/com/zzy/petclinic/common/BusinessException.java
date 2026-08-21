package com.zzy.petclinic.common;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
  private final HttpStatus status;

  public BusinessException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public static BusinessException notFound(String name) {
    return new BusinessException(HttpStatus.NOT_FOUND, name + "不存在");
  }

  public static BusinessException conflict(String message) {
    return new BusinessException(HttpStatus.CONFLICT, message);
  }
}
