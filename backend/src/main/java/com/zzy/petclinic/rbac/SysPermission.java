package com.zzy.petclinic.rbac;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zzy.petclinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SysPermission extends BaseEntity {
  private Long parentId;
  private String code;
  private String name;
  private String type;
  private String path;
  private String icon;
  private Integer sortOrder;
  private String status;
}
