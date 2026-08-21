package com.zzy.petclinic.medical;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zzy.petclinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("medical_record")
public class MedicalRecord extends BaseEntity {
  private Long visitId;
  private Long petId;
  private Long vetId;
  private String symptoms;
  private String diagnosis;
  private String treatment;
  private String prescription;
  private String notes;
}
