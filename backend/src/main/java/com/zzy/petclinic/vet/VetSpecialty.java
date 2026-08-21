package com.zzy.petclinic.vet;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("vet_specialty")
public class VetSpecialty {
  @TableId(value = "vet_id", type = IdType.INPUT)
  private Long vetId;

  private Long specialtyId;
}
