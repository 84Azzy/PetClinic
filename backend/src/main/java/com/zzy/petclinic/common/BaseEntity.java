package com.zzy.petclinic.common;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public abstract class BaseEntity {
  @TableId(type = IdType.AUTO)
  private Long id;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
