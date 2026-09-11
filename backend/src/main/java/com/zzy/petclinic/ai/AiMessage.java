package com.zzy.petclinic.ai;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zzy.petclinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 会话中的单条持久化消息。
 *
 * <p>{@code role} 可为 USER、TOOL 或 ASSISTANT；TOOL 消息的 payload 只保存最小审计摘要，
 * ASSISTANT 消息的 payload 可保存预约草稿 JSON。
 */
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
