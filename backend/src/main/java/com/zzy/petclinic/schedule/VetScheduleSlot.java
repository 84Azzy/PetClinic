package com.zzy.petclinic.schedule;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zzy.petclinic.common.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vet_schedule_slot")
public class VetScheduleSlot extends BaseEntity {
  private Long vetId;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private String status;
  private String note;
}
