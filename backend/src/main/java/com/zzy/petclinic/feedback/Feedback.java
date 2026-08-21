package com.zzy.petclinic.feedback;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zzy.petclinic.common.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("feedback")
public class Feedback extends BaseEntity {
  private Long userId;
  private String category;
  private String title;
  private String content;
  private String contact;
  private String status;
  private String reply;
  private Long repliedBy;
  private LocalDateTime repliedAt;
}
