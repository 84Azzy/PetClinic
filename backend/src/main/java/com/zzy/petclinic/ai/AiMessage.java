package com.zzy.petclinic.ai;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zzy.petclinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_message")
public class AiMessage extends BaseEntity {
  private Long conversationId;
  private String role;
  private String content;
  private String toolName;
  private String toolPayload;
}
