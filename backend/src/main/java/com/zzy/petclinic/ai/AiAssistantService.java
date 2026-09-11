package com.zzy.petclinic.ai;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * AI 对话用例的业务契约。
 *
 * <p>所有方法都以当前安全上下文中的用户为边界，不接受客户端传入的 userId。
 */
@PreAuthorize("hasAuthority('ai:chat')")
public interface AiAssistantService {

  /**
   * 创建新会话或续聊现有会话。
   *
   * @param request 聊天请求
   * @return 结构化聊天响应
   */
  AiContracts.ChatResponse chat(AiContracts.ChatRequest request);

  /**
   * 查询当前用户的有效会话。
   *
   * @return 会话列表
   */
  List<AiConversation> conversations();

  /**
   * 查询当前用户拥有的指定会话消息。
   *
   * @param conversationId 会话编号
   * @return 消息列表
   */
  List<AiMessage> messages(Long conversationId);

  /**
   * 软删除当前用户拥有的指定会话。
   *
   * @param conversationId 会话编号
   */
  void delete(Long conversationId);
}
