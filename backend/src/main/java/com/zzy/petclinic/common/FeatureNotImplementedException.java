package com.zzy.petclinic.common;

public class FeatureNotImplementedException extends BusinessException {
  public FeatureNotImplementedException(String feature) {
    super(org.springframework.http.HttpStatus.NOT_IMPLEMENTED, feature + "已定义接口契约，业务实现留给学习者完成");
  }
}
