package com.zzy.petclinic.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * AI 模块的 HTTP 和模型结构化输出契约集合。
 *
 * <p>该类只作为命名空间，不保存状态。
 */
public final class AiContracts {
  private AiContracts() {}

  /**
   * conversationId 为 null 表示新建会话；
   * 有值表示继续当前用户已有的某个会话。
   *
   * @param conversationId 可选会话编号
   * @param message 当前用户消息
   */
  public record ChatRequest(
          Long conversationId,
          @NotBlank @Size(max = 2000) String message) {}

  /**
   * AI 只能生成草稿，不能直接创建 Visit。
   *
   * @param petId 已校验属于当前用户的宠物编号
   * @param slotId 尚可预约的时段编号
   * @param reason 就诊原因
   * @param summary 供用户确认的摘要
   */
  public record AppointmentDraft(
          @NotNull Long petId,
          @NotNull Long slotId,
          @NotBlank @Size(max = 500) String reason,
          @Size(max = 500) String summary) {}

  /**
   * DeepSeek 最终必须输出的 JSON 结构。
   *
   * @param answer 给用户展示的中文回答
   * @param draft 可选预约草稿
   */
  public record ModelReply(
          @NotBlank @Size(max = 4000) String answer,
          @Valid AppointmentDraft draft) {}

  /**
   * 返回给前端的聊天结果。
   *
   * @param conversationId 新建或续聊的会话编号
   * @param answer AI 回答
   * @param draft 可选预约草稿
   * @param toolsUsed 本轮实际执行过的去重工具名称
   */
  public record ChatResponse(
          Long conversationId,
          String answer,
          AppointmentDraft draft,
          List<String> toolsUsed) {}
}
