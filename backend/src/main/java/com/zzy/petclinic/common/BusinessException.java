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

  /**
   * 400异常 坏请求
   * @param msg
   * @return
   */
  public static BusinessException badRequest(String msg){
    return new BusinessException(HttpStatus.BAD_REQUEST,msg);
  }

  /**
   * 404异常 找不到
   * @param name
   * @return
   */
  public static BusinessException notFound(String name) {
    return new BusinessException(HttpStatus.NOT_FOUND, name + "不存在");
  }

  /**
   * 409异常 冲突
   * @param message
   * @return
   */
  public static BusinessException conflict(String message) {
    return new BusinessException(HttpStatus.CONFLICT, message);
  }
}
