package com.zzy.petclinic.ai;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 会话的 MyBatis 持久化接口。
 *
 * <p>除 MyBatis-Plus 基础 CRUD 外，自定义 SQL 都将 userId 和 ACTIVE 状态写入条件，防止越权读写。
 */
public interface AiConversationMapper extends BaseMapper<AiConversation> {

    /**
     * 查询指定用户拥有的有效会话。
     *
     * @param id 会话编号
     * @param userId 用户编号
     * @return 命中的会话，未命中时为 {@code null}
     */
    AiConversation selectOwnedActive(
            @Param("id")Long id,
            @Param("userId")Long userId
    );

    /**
     * 查询指定用户的全部有效会话。
     *
     * @param userId 用户编号
     * @return 按最近更新时间倒序排列的会话
     */
    List<AiConversation> selectActiveByUserId(@Param("userId")Long userId);

    /**
     * 在归属和状态条件成立时刷新会话活跃时间。
     *
     * @param id 会话编号
     * @param userId 用户编号
     * @param updatedAt 新的更新时间
     * @return 受影响行数
     */
    int touchOwned(
            @Param("id")Long id,
            @Param("userId")Long userId,
            // 不恰当：实体字段名为 updatedAt，原参数名 updateAt 容易与字段约定混淆。
            // @Param("updateAt")LocalDateTime updateAt
            @Param("updatedAt")LocalDateTime updatedAt
    );

    /**
     * 在归属和状态条件成立时将会话标记为已删除。
     *
     * @param id 会话编号
     * @param userId 用户编号
     * @param updatedAt 删除操作时间
     * @return 受影响行数
     */
    int softDeleteOwned(
            @Param("id") Long id,
            @Param("userId")Long userId,
            // @Param("updateAt") LocalDateTime updateAt
            @Param("updatedAt") LocalDateTime updatedAt
    );
}
