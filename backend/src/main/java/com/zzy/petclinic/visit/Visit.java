package com.zzy.petclinic.visit;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zzy.petclinic.common.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("visit")
public class Visit extends BaseEntity {
  private Long petId;
  private Long slotId;
  private Long vetId;
  private Long createdBy;
  private String requestId;
  private String reason;
  private String status;
  private LocalDateTime cancelledAt;
  private String cancelReason;
}
