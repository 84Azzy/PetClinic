package com.zzy.petclinic.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class AiContracts {
  private AiContracts() {}

  /**
   * conversationId 为 null 表示新建会话；
   * 有值表示继续当前用户已有的某个会话。
   */
  public record ChatRequest(
          Long conversationId,
          @NotBlank @Size(max = 2000) String message) {}

  /**
   * AI 只能生成草稿，不能直接创建 Visit。
   */
  public record AppointmentDraft(
          @NotNull Long petId,
          @NotNull Long slotId,
          @NotBlank @Size(max = 500) String reason,
          @Size(max = 500) String summary) {}

  /**
   * DeepSeek 最终必须输出的 JSON 结构。
   */
  public record ModelReply(
          @NotBlank @Size(max = 4000) String answer,
          @Valid AppointmentDraft draft) {}

  public record ChatResponse(
          Long conversationId,
          String answer,
          AppointmentDraft draft,
          List<String> toolsUsed) {}
}