package com.zzy.petclinic.vet;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zzy.petclinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vet")
public class Vet extends BaseEntity {
  private String name;
  private String phone;
  private String email;
  private String licenseNo;
  private String biography;
  private String avatarUrl;
  private String status;
}
