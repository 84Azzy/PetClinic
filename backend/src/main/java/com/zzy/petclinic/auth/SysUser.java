package com.zzy.petclinic.auth;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zzy.petclinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {
  private String username;
  private String passwordHash;
  private String displayName;
  private String phone;
  private String email;
  private String accountType;
  private String status;
  private Integer tokenVersion;
}
