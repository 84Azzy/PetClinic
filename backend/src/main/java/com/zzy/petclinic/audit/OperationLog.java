package com.zzy.petclinic.audit;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zzy.petclinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("operation_log")
public class OperationLog extends BaseEntity {
  private Long userId;
  private String username;
  private String module;
  private String operation;
  private String httpMethod;
  private String requestUri;
  private Integer responseStatus;
  private Long durationMs;
  private String ipAddress;
  private String errorMessage;
}
