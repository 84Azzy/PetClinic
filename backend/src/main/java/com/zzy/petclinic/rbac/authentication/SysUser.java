package com.zzy.petclinic.rbac.authentication;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zzy.petclinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {
  private String username;
  // 不恰当：密码哈希作为普通属性参与 JSON 序列化，后台接口可能把它返回给前端。
  // private String passwordHash;
  @JsonIgnore
  private String passwordHash;
  private String displayName;
  private String phone;
  private String email;
  private String accountType;
  private String status;
  private Integer tokenVersion;
}
