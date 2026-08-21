package com.zzy.petclinic.catalog;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zzy.petclinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("specialty")
public class Specialty extends BaseEntity {
  private String name;
  private String description;
  private String status;
}
