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

@Service
@RequiredArgsConstructor
public class AiConversationStore {

    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    public AiConversation requiredOwnedActive(Long conversationId, Long userId){
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

    public List<AiConversation> conversations(Long userId){
        return conversationMapper.selectActiveByUserId(userId);
    }

    public List<AiMessage> messages(Long conversationId, Long userId){
        requiredOwnedActive(conversationId,userId);
        return messageMapper.selectOwnedMessages(conversationId,userId);
    }

    public List<AiMessage> recentContext(
            Long conversationId,
            Long userId,
            int limit){
        requiredOwnedActive(conversationId,userId);
        return messageMapper.selectOwnedContext(conversationId,userId,limit);
    }

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

    @Transactional
    public void delete(Long conversationId,Long userId){
        requiredOwnedActive(conversationId,userId);
        if(conversationMapper.softDeleteOwned(conversationId,userId,LocalDateTime.now())!=1){
            throw BusinessException.conflict("AI删除会话失败，请重试");
        }
    }

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

    private String toJson(Object value){
        try{
            return objectMapper.writeValueAsString(value);
        }catch (JsonProcessingException exception){
            throw new IllegalStateException("序列化AI消息失败",exception);
        }
    }
}
