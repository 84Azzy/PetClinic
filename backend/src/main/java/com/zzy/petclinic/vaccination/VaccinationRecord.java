package com.zzy.petclinic.vaccination;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zzy.petclinic.common.BaseEntity;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vaccination_record")
public class VaccinationRecord extends BaseEntity {
  private Long petId;
  private String vaccineName;
  private String batchNo;
  private LocalDate vaccinatedDate;
  private LocalDate nextDueDate;
  private String veterinarian;
  private String status;
  private String notes;
}
