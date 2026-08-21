package com.zzy.petclinic.owner;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zzy.petclinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("owner")
public class Owner extends BaseEntity {
  private Long userId;
  private String name;
  private String phone;
  private String email;
  private String address;
  private String status;
}
