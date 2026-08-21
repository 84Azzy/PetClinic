package com.zzy.petclinic.notice;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zzy.petclinic.common.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notice")
public class Notice extends BaseEntity {
  private String title;
  private String content;
  private String status;
  private Long publisherId;
  private LocalDateTime publishedAt;
  private LocalDateTime expiresAt;
  private Integer sortOrder;
}
