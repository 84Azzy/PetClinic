package com.zzy.petclinic.visit;

import com.zzy.petclinic.common.*;

/**
 * 预约业务契约。
 *
 * <p>由 {@link VisitServiceImpl} 实现。权限、资源归属、幂等和状态流转都在实现类中校验，Controller 只负责转发请求。
 */
public interface VisitService {
  /**
   * 创建预约。
   *
   * <p>实现提示（整个方法需要放在一个 {@code @Transactional} 事务中）：
   *
   * <ol>
   *   <li>从 {@code CurrentUser} 取得当前用户编号，不接收客户端提供的用户编号。</li>
   *   <li>按 {@code requestId} 查询旧预约。同一用户用相同参数重试时可直接返回旧结果；创建人或参数不一致时返回 409。</li>
   *   <li>查询 Pet，校验它存在、状态为 ACTIVE，并通过 {@code pet.owner_id -> owner.user_id} 确认属于当前用户；不存在返回 404，越权返回 403。</li>
   *   <li>查询 Slot，确认它存在，并从 Slot 取得可信的 {@code vetId}。</li>
   *   <li>调用 {@link VisitMapper#claimSlot(Long)} 条件抢占时段；返回值不是 1 时抛出 409。</li>
   *   <li>组装 Visit：设置 petId、slotId、从时段取得的 vetId、当前用户 createdBy、requestId、reason、SCHEDULED 及创建/更新时间，然后插入。</li>
   *   <li>返回插入后的预约。插入或后续步骤抛异常时让事务回滚，不要吞掉异常，否则时段会一直停留在 BOOKED。</li>
   * </ol>
   *
   * <p>数据库的 request_id 和 slot_id 唯一约束是最后一道并发保护；实现时还应把对应的重复键异常转换成可读的 409。
   *
   * @param request 已通过 Bean Validation 基础校验的创建参数
   * @return 新创建的预约，或幂等重试对应的原预约
   */
  Visit create(VisitRequest request);

  /**
   * 分页查询预约。
   *
   * <p>实现提示：用 MyBatis-Plus 的 {@code Page} 和条件构造器实现；petId、vetId、keyword、status 仅在有值时拼接，结果建议按 id
   * 倒序。管理员/员工可查询全部数据，宠物主人必须额外限定 {@code created_by = 当前用户ID}，不能只靠前端隐藏数据。
   *
   * @param query 页码、每页数量、关键词和状态
   * @param petId 可选的宠物筛选条件
   * @param vetId 可选的兽医筛选条件
   * @return 分页结果
   */
  PageResponse<Visit> page(PageQuery query, Long petId, Long vetId);

  /**
   * 查询当前登录用户创建的预约。
   *
   * <p>实现提示：从 {@code CurrentUser.id()} 取用户编号后调用 {@link VisitMapper#selectMine(Long)}，不要把 userId 暴露成接口参数。
   *
   * @return 当前用户的预约列表
   */
  java.util.List<Visit> mine();

  /**
   * 查询预约详情。
   *
   * <p>实现提示：先按主键查询，不存在返回 404；管理员/员工可以读取，宠物主人只能读取 {@code createdBy} 等于当前用户编号的记录，
   * 否则返回 403。
   *
   * @param id 预约编号
   * @return 预约详情
   */
  Visit get(Long id);

  /**
   * 取消预约并释放时段。
   *
   * <p>实现提示（整个方法需要放在一个 {@code @Transactional} 事务中）：
   *
   * <ol>
   *   <li>查询预约；不存在返回 404。</li>
   *   <li>确认 {@code createdBy} 等于当前用户编号；不能取消他人的预约，否则返回 403。</li>
   *   <li>只有 SCHEDULED 可以取消；已经 CANCELLED 或 COMPLETED 时返回 409。</li>
   *   <li>把状态改为 CANCELLED，同时写入 {@code cancelledAt}、{@code cancelReason} 和 {@code updatedAt}。</li>
   *   <li>调用 {@link VisitMapper#releaseSlot(Long)} 把时段从 BOOKED 恢复为 AVAILABLE，并检查受影响行数为 1。</li>
   *   <li>保存并返回预约。任一步失败都应抛异常，让预约和时段一起回滚。</li>
   * </ol>
   *
   * @param id 预约编号
   * @param request 取消原因
   * @return 已取消的预约
   */
  Visit cancel(Long id, CancelVisitRequest request);

  /**
   * 将待就诊预约标记为已完成。
   *
   * <p>实现提示：该操作只应授权给员工或管理员。先查询预约，不存在返回 404；只有 SCHEDULED 能变为 COMPLETED，已取消或已完成返回
   * 409；更新状态和 {@code updatedAt} 后保存。完成就诊不释放时段，因为该时段已经实际使用。
   *
   * @param id 预约编号
   * @return 已完成的预约
   */
  Visit complete(Long id);
}
