package com.zzy.petclinic.ai;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiMessageMapper extends BaseMapper<AiMessage> {

    List<AiMessage> selectOwnedMessages(
            @Param("conversationId")Long conversationId,
            @Param("userId") Long userId
    );

    List<AiMessage> selectOwnedContext(
            @Param("conversationId") Long conversationId,
            @Param("userId")Long userId,
            @Param("limit")int limit
    );
}
