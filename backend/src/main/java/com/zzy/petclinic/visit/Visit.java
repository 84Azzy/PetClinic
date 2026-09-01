package com.zzy.petclinic.visit;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zzy.petclinic.common.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 一次预约就诊记录，对应数据库中的 {@code visit} 表。
 *
 * <p>正常状态流转为 {@code SCHEDULED -> COMPLETED} 或 {@code SCHEDULED -> CANCELLED}。取消预约时还要在同一事务中把关联时段
 * 从 {@code BOOKED} 恢复为 {@code AVAILABLE}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("visit")
public class Visit extends BaseEntity {
  /** 本次就诊的宠物编号。 */
  private Long petId;

  /** 被预约的排班时段编号；数据库唯一约束保证一个时段最多对应一条预约。 */
  private Long slotId;

  /** 接诊兽医编号；创建预约时从排班时段中读取，不能由客户端指定。 */
  private Long vetId;

  /** 创建预约的系统用户编号；必须来自当前认证用户。 */
  private Long createdBy;

  /** 创建请求的幂等键；相同请求重试时用于找回原预约。 */
  private String requestId;

  /** 用户填写的就诊原因。 */
  private String reason;

  /** 预约状态：SCHEDULED（待就诊）、COMPLETED（已完成）或 CANCELLED（已取消）。 */
  private String status;

  /** 取消时间；只有状态为 CANCELLED 时才有值。 */
  private LocalDateTime cancelledAt;

  /** 取消原因；只有状态为 CANCELLED 时才有值。 */
  private String cancelReason;
}
