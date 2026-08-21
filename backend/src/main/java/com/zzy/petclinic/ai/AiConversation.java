package com.zzy.petclinic.ai;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zzy.petclinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_conversation")
public class AiConversation extends BaseEntity {
  private Long userId;
  private String title;
  private String status;
}
