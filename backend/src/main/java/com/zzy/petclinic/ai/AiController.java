package com.zzy.petclinic.ai;

import com.zzy.petclinic.common.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AI 对话的 HTTP 入口。
 *
 * <p>Controller 只负责参数校验、路由和统一响应包装；会话归属、模型调用、工具调用和草稿校验均由
 * {@link AiAssistantService} 处理。
 */
@Tag(name = "AI 助手（学习者实现）")
@RestController
@RequestMapping("/api/ai")
@PreAuthorize("hasAuthority('ai:chat')")
@RequiredArgsConstructor
public class AiController {

  private final AiAssistantService aiAssistantService;

  /**
   * 新建或续聊一个 AI 会话。
   *
   * @param r 用户消息和可选会话编号
   * @return AI 回答和可选预约草稿
   */
  @PostMapping("/chat")
  public ApiResponse<AiContracts.ChatResponse> chat(@Valid @RequestBody AiContracts.ChatRequest r) {
    // Day2 占位代码：throw todo();
    return ApiResponse.ok(aiAssistantService.chat(r));
  }

  /**
   * 查询当前用户的有效 AI 会话。
   *
   * @return 当前用户的会话列表
   */
  @GetMapping("/conversations")
  public ApiResponse<List<AiConversation>> conversations() {
    // Day2 占位代码：throw todo();
    return ApiResponse.ok(aiAssistantService.conversations());
  }

  /**
   * 查询当前用户拥有的某个 AI 会话的消息。
   *
   * @param id 会话编号
   * @return 按创建顺序排列的消息列表
   */
  @GetMapping("/conversations/{id}/messages")
  public ApiResponse<List<AiMessage>> messages(@PathVariable Long id) {
    // Day2 占位代码：throw todo();
    return ApiResponse.ok(aiAssistantService.messages(id));
  }

  /**
   * 软删除当前用户拥有的 AI 会话。
   *
   * @param id 会话编号
   * @return 统一成功消息
   */
  @DeleteMapping("/conversations/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    // Day2 占位代码：throw todo();
    aiAssistantService.delete(id);
    return ApiResponse.message("会话已删除");
  }
}
