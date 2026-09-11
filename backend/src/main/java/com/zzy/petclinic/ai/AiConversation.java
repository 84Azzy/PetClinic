package com.zzy.petclinic.ai;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zzy.petclinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 一个用户独立的 AI 会话实体。
 *
 * <p>{@code userId} 用于资源归属校验；{@code status} 使用 ACTIVE/DELETED 实现软删除。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_conversation")
public class AiConversation extends BaseEntity {
  private Long userId;
  private String title;
  private String status;
}
