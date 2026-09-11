package com.zzy.petclinic.ai;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI 消息的 MyBatis 持久化接口。
 *
 * <p>查询会通过 ai_conversation 联表同时校验会话归属和有效状态。
 */
public interface AiMessageMapper extends BaseMapper<AiMessage> {

    /**
     * 查询当前用户拥有会话的全部消息。
     *
     * @param conversationId 会话编号
     * @param userId 用户编号
     * @return 按创建顺序排列的消息
     */
    List<AiMessage> selectOwnedMessages(
            @Param("conversationId")Long conversationId,
            @Param("userId") Long userId
    );

    /**
     * 查询会话最近的 USER/ASSISTANT 消息并以正序返回，供模型续聊。
     *
     * @param conversationId 会话编号
     * @param userId 用户编号
     * @param limit 上下文最大消息条数
     * @return 按时间正序排列的最近消息
     */
    List<AiMessage> selectOwnedContext(
            @Param("conversationId") Long conversationId,
            @Param("userId")Long userId,
            @Param("limit")int limit
    );
}
