package com.zzy.petclinic.ai;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.security.core.parameters.P;

import java.time.LocalDateTime;
import java.util.List;

public interface AiConversationMapper extends BaseMapper<AiConversation> {

    AiConversation selectOwnedActive(
            @Param("id")Long id,
            @Param("userId")Long userId
    );

    List<AiConversation> selectActiveByUserId(@Param("userId")Long userId);

    int touchOwned(
            @Param("id")Long id,
            @Param("userId")Long userId,
            @Param("updateAt")LocalDateTime updateAt
    );

    int softDeleteOwned(
            @Param("id") Long id,
            @Param("userId")Long userId,
            @Param("updateAt") LocalDateTime updateAt
    );
}
