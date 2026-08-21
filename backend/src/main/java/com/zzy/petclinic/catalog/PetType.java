package com.zzy.petclinic.catalog;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zzy.petclinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pet_type")
public class PetType extends BaseEntity {
  private String name;
  private String description;
  private String status;
}
