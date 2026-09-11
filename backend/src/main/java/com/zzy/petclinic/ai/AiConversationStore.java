package com.zzy.petclinic.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zzy.petclinic.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * AI 会话和消息的持久化边界。
 *
 * <p>该类统一执行会话归属校验，并将一轮 USER/TOOL/ASSISTANT 消息放在同一个短事务中写入。
 * 模型网络请求不在本类事务内执行，避免长时间占用数据库连接。
 */
@Service
@RequiredArgsConstructor
public class AiConversationStore {

    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    /**
     * 要求目标会话存在、未删除且属于指定用户。
     *
     * <p>先用一条带归属和状态条件的 SQL 查询；未命中时再查主键，以区分 404、403 和并发状态变化。
     *
     * @param conversationId 会话编号
     * @param userId 当前登录用户编号
     * @return 已通过归属和状态校验的会话
     * @throws BusinessException 会话不存在、已删除或状态并发变化时抛出
     * @throws AccessDeniedException 会话属于其他用户时抛出
     */
    // 不恰当：required 表示「被需要的」；这个方法是执行「要求/校验」，动词应使用 require。
    // public AiConversation requiredOwnedActive(Long conversationId, Long userId){
    public AiConversation requireOwnedActive(Long conversationId, Long userId){
        AiConversation owned = conversationMapper.selectOwnedActive(conversationId,userId);
        if(owned!=null){
            return owned;
        }
        AiConversation existing = conversationMapper.selectById(conversationId);
        if(existing==null || !"ACTIVE".equals(existing.getStatus())){
            throw BusinessException.notFound("AI会话");
        }
        if(!Objects.equals(existing.getUserId(),userId)){
            throw new AccessDeniedException("不能访问其他用户的会话");
        }
        throw BusinessException.conflict("AI会话状态已经发生变化");
    }

    /**
     * 按最近活跃时间查询指定用户的全部有效会话。
     *
     * @param userId 当前登录用户编号
     * @return 当前用户的 ACTIVE 会话列表
     */
    public List<AiConversation> conversations(Long userId){
        return conversationMapper.selectActiveByUserId(userId);
    }

    /**
     * 查询某会话的全部消息，供前端恢复对话展示。
     *
     * @param conversationId 会话编号
     * @param userId 当前登录用户编号
     * @return 按创建顺序排列的消息
     */
    public List<AiMessage> messages(Long conversationId, Long userId){
        // requiredOwnedActive(conversationId,userId);
        requireOwnedActive(conversationId,userId);
        return messageMapper.selectOwnedMessages(conversationId,userId);
    }

    /**
     * 读取最近的 USER/ASSISTANT 消息作为模型上下文。
     *
     * @param conversationId 会话编号
     * @param userId 当前登录用户编号
     * @param limit 最多返回的消息条数
     * @return 最近消息，对外仍按时间正序排列
     */
    public List<AiMessage> recentContext(
            Long conversationId,
            Long userId,
            int limit){
        // requiredOwnedActive(conversationId,userId);
        requireOwnedActive(conversationId,userId);
        return messageMapper.selectOwnedContext(conversationId,userId,limit);
    }

    /**
     * 在一个事务中创建新会话，并写入它的第一轮完整消息。
     *
     * @param userId 会话所属的用户编号
     * @param title 从首条用户消息截取的标题
     * @param userText 用户原始消息
     * @param toolUses 本轮工具调用概要
     * @param reply 已验证的模型回复
     * @return 已持久化且包含主键的新会话
     */
    @Transactional
    public AiConversation createTurn(
            Long userId,
            String title,
            String userText,
            List<AiToolCallRecorder.ToolUse>toolUses,
            AiContracts.ModelReply reply){
        LocalDateTime now = LocalDateTime.now();
        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setTitle(title);
        conversation.setStatus("ACTIVE");
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        if(conversationMapper.insert(conversation)!=1){
            throw new IllegalStateException("创建AI会话失败");
        }
        insertTurn(conversation.getId(),userText,toolUses,reply,now);
        return conversation;
    }

    /**
     * 在一个事务中更新会话活跃时间，并追加一轮完整消息。
     *
     * @param conversationId 会话编号
     * @param userId 当前登录用户编号
     * @param userText 用户原始消息
     * @param toolUses 本轮工具调用概要
     * @param reply 已验证的模型回复
     */
    @Transactional
    public void appendTurn(
            Long conversationId,
            Long userId,
            String userText,
            List<AiToolCallRecorder.ToolUse>toolUses,
            AiContracts.ModelReply reply){
        LocalDateTime now = LocalDateTime.now();
        if(conversationMapper.touchOwned(conversationId,userId,now)!=1){
            throw BusinessException.notFound("AI会话");
        }
        insertTurn(conversationId,userText,toolUses,reply,now);
    }

    /**
     * 软删除当前用户拥有的有效会话。
     *
     * @param conversationId 会话编号
     * @param userId 当前登录用户编号
     */
    @Transactional
    public void delete(Long conversationId,Long userId){
        // requiredOwnedActive(conversationId,userId);
        requireOwnedActive(conversationId,userId);
        if(conversationMapper.softDeleteOwned(conversationId,userId,LocalDateTime.now())!=1){
            throw BusinessException.conflict("AI删除会话失败，请重试");
        }
    }

    /**
     * 按 USER、零到多条 TOOL、ASSISTANT 的顺序写入一轮消息。
     *
     * @param conversationId 会话编号
     * @param userText 用户消息
     * @param toolUses 工具调用概要
     * @param reply 模型回复
     * @param now 本轮共享的写入时间
     */
    private void insertTurn(
            Long conversationId,
            String userText,
            List<AiToolCallRecorder.ToolUse>toolUses,
            AiContracts.ModelReply reply,
            LocalDateTime now){
        insertMessage(conversationId,"USER",userText,null,null,now);
        for(AiToolCallRecorder.ToolUse use:toolUses){
            insertMessage(conversationId,"TOOL",
                    use.success()?"工具调用成功":"工具调用失败",
                    use.name(),
                    toJson(use),
                    now);
        }
        insertMessage(
                conversationId, "ASSISTANT",
                reply.answer(),
                null,
                reply.draft() == null ? null : toJson(reply.draft()),
                now);
    }

    /**
     * 构造并插入单条 AI 消息，插入行数异常时中止当前事务。
     *
     * @param conversationId 会话编号
     * @param role 消息角色
     * @param content 用于展示的文本内容
     * @param toolName 工具名，非 TOOL 消息为 {@code null}
     * @param toolPayload 工具审计或预约草稿 JSON
     * @param now 写入时间
     */
    private void insertMessage(
            Long conversationId,
            String role,
            String content,
            String toolName,
            String toolPayload,
            LocalDateTime now){
        AiMessage message = new AiMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setToolName(toolName);
        message.setToolPayload(toolPayload);
        message.setCreatedAt(now);
        message.setUpdatedAt(now);

        if (messageMapper.insert(message) != 1) {
            throw new IllegalStateException("保存 AI 消息失败");
        }
    }

    /**
     * 将工具审计或预约草稿序列化为可入库的 JSON。
     *
     * @param value 待序列化对象
     * @return JSON 字符串
     */
    private String toJson(Object value){
        try{
            return objectMapper.writeValueAsString(value);
        }catch (JsonProcessingException exception){
            throw new IllegalStateException("序列化AI消息失败",exception);
        }
    }
}
