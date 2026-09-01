package com.zzy.petclinic.visit;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 预约模块的数据访问接口。
 *
 * <p>{@link BaseMapper} 已提供按主键查询、插入和更新等通用方法；这里的方法需要在 {@code mapper/VisitMapper.xml} 中补充 SQL。
 */
public interface VisitMapper extends BaseMapper<Visit> {
  /**
   * 按幂等键查询已经创建的预约。
   *
   * <p>SQL 要点：使用 {@code WHERE request_id = #{requestId}} 精确匹配。数据库已有唯一约束，因此最多返回一条记录。
   *
   * @param requestId 客户端生成的幂等键
   * @return 已存在的预约；不存在时返回 {@code null}
   */
  Visit selectByRequestId(String requestId);

  /**
   * 查询指定用户创建的全部预约。
   *
   * <p>SQL 要点：使用 {@code WHERE created_by = #{userId}} 隔离用户数据，并按编号或创建时间倒序排列。
   *
   * @param userId 当前认证用户编号，不能从请求参数中获取
   * @return 该用户的预约列表；没有记录时返回空列表
   */
  List<Visit> selectMine(@Param("userId")Long userId);

  /**
   * 原子抢占一个仍然可用的排班时段。
   *
   * <p>不能先查询 AVAILABLE 再无条件更新，否则两个并发请求可能同时成功。SQL 应采用类似：
   * {@code UPDATE vet_schedule_slot SET status = 'BOOKED', updated_at = NOW() WHERE id = #{slotId} AND status = 'AVAILABLE'}。
   *
   * @param slotId 排班时段编号
   * @return 受影响行数；只有返回 1 才表示抢占成功，返回 0 表示时段不存在或已不可预约
   */
  int claimSlot(@Param("slotId") Long slotId);

  /**
   * 取消预约时释放已经占用的排班时段。
   *
   * <p>SQL 应只把指定时段从 BOOKED 改回 AVAILABLE，并同步更新时间。Service 必须在更新预约状态的同一事务中调用本方法。
   *
   * @param slotId 排班时段编号
   * @return 受影响行数；正常取消时应为 1
   */
  int releaseSlot(@Param("slotId") Long slotId);
}
