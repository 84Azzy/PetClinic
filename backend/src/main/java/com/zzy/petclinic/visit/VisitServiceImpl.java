package com.zzy.petclinic.visit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzy.petclinic.authentication.AuthenticatedUser;
import com.zzy.petclinic.authentication.CurrentUser;
import com.zzy.petclinic.common.BusinessException;
import com.zzy.petclinic.common.PageQuery;
import com.zzy.petclinic.common.PageResponse;
import com.zzy.petclinic.owner.Owner;
import com.zzy.petclinic.owner.OwnerService;
import com.zzy.petclinic.pet.Pet;
import com.zzy.petclinic.pet.PetService;
import com.zzy.petclinic.schedule.ScheduleService;
import com.zzy.petclinic.schedule.VetScheduleSlot;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class VisitServiceImpl implements VisitService {

    private final VisitMapper visitMapper;
    private final CurrentUser currentUser;
    private final OwnerService ownerService;
    private final PetService petService;
    private final ScheduleService scheduleService;

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
    @Override
    @Transactional
    public Visit create(VisitRequest request) {
        AuthenticatedUser user = currentUser.require();
        Long userId = user.getId();

        /*
         * 原代码：只要创建人相同就直接返回旧预约，没有比较本次请求的其他参数。
         * Visit last = visitMapper.selectByRequestId(request.requestId());
         * if(last !=null){
         *     if(last.getCreatedBy().equals(userId)){
         *         return last;
         *     }else{
         *         throw new BusinessException(409,"创建人和参数不一致");
         *     }
         * }
         */
        // 按 requestId 查旧预约。只有“同一个用户、同一组参数”才属于安全的幂等重试。
        Visit last = visitMapper.selectByRequestId(request.requestId());
        if (last != null) {
            if (Objects.equals(last.getCreatedBy(), userId)
                    && Objects.equals(last.getPetId(), request.petId())
                    && Objects.equals(last.getSlotId(), request.slotId())
                    && Objects.equals(last.getReason(), request.reason())) {
                return last;
            }
            throw BusinessException.conflict("requestId 已被其他预约使用");
        }

        /*
         * 原代码：pet 为 null 的判断实际上不会执行，因为 PetService.get 不存在时已经抛出 404。
         * Pet pet = petService.get(request.petId());
         * if(pet==null || !pet.getStatus().equals("ACTIVE")){
         *     throw new BusinessException(HttpStatus.NOT_FOUND,"宠物不存在或已删除");
         * }
         * Owner owner = ownerService.mine();
         * if(!pet.getOwnerId().equals(owner.getId())){
         *     throw new AccessDeniedException("无权访问其他主人宠物信息");
         * }
         */
        // 查询并校验宠物。PetService 负责存在性和当前用户的资源归属校验。
        Pet pet = petService.get(request.petId());
        if (!"ACTIVE".equals(pet.getStatus())) {
            throw BusinessException.conflict("宠物已停用，不能创建预约");
        }
        Owner owner = ownerService.mine();
        if (!Objects.equals(pet.getOwnerId(), owner.getId())) {
            throw new AccessDeniedException("无权访问其他主人宠物信息");
        }

        /*
         * 原代码：ScheduleService.get 同样已经负责“不存在返回 404”，无需再次判断 null。
         * VetScheduleSlot slot = scheduleService.get(request.slotId());
         * if(slot==null){
         *     throw new BusinessException(HttpStatus.NOT_FOUND,"预约时段不存在");
         * }
         * Long vetId = slot.getVetId();
         * int res = visitMapper.claimSlot(request.slotId());
         * if(res!=1){
         *     throw new BusinessException(409,"抢占时段失败");
         * }
         */
        // 查询时段并取得可信的 vetId；真正的可用性由下面的条件更新判断。
        VetScheduleSlot slot = scheduleService.get(request.slotId());
        Long vetId = slot.getVetId();
        //todo 核心代码，利用mysql的条件更新把查询可用时段和进行抢占合到一条sql中，避免并发时一个时段多人预约成功
        int res = visitMapper.claimSlot(request.slotId());
        if (res != 1) {
            throw BusinessException.conflict("该时段不存在或已不可预约");
        }

        // 组装预约；身份和兽医编号都来自服务端可信数据。
        LocalDateTime now = LocalDateTime.now();
        Visit visit = new Visit();
        visit.setPetId(pet.getId());
        visit.setSlotId(slot.getId());
        visit.setVetId(vetId);
        visit.setCreatedBy(userId);
        visit.setRequestId(request.requestId());
        visit.setReason(request.reason());
        visit.setStatus("SCHEDULED");

        /*
         * 原代码：没有设置 updatedAt，也没有检查插入行数和处理数据库唯一键冲突。
         * visit.setCreatedAt(LocalDateTime.now());
         * visitMapper.insert(visit);
         */
        visit.setCreatedAt(now);
        visit.setUpdatedAt(now);
        try {
            if (visitMapper.insert(visit) != 1) {
                throw BusinessException.conflict("预约创建失败，请重试");
            }
        } catch (DuplicateKeyException exception) {
            // request_id/slot_id 的数据库唯一约束是并发情况下的最后一道保护。
            throw BusinessException.conflict("requestId 或预约时段已被使用");
        }
        return visit;
    }

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
    @Override
    public PageResponse<Visit> page(PageQuery query, Long petId, Long vetId) {
        /*
         * 原代码：没有指定排序。
         * LambdaQueryWrapper<Visit> wrapper = new LambdaQueryWrapper<>();
         */
        Page<Visit> page = new Page<>(query.pageValue(), query.sizeValue());
        LambdaQueryWrapper<Visit> wrapper =
                new LambdaQueryWrapper<Visit>().orderByDesc(Visit::getId);
        AuthenticatedUser user = currentUser.require();
        if ("OWNER".equals(user.getAccountType())) {
            wrapper.eq(Visit::getCreatedBy, user.getId());
        }
        if (petId != null) {
            wrapper.eq(Visit::getPetId, petId);
        }
        if (vetId != null) {
            wrapper.eq(Visit::getVetId, vetId);
        }

        /*
         * 原代码：把 keyword 错当成 status；不传 keyword 时又强制只查 COMPLETED。
         * if(query.keyword() !=null && !query.keyword().isBlank()){
         *     wrapper.eq(Visit::getStatus,query.keyword());
         * }else{
         *     wrapper.eq(Visit::getStatus,"COMPLETED");
         * }
         */
        if (StringUtils.hasText(query.keyword())) {
            wrapper.and(
                    value ->
                            value.like(Visit::getReason, query.keyword())
                                    .or()
                                    .like(Visit::getRequestId, query.keyword()));
        }
        if (StringUtils.hasText(query.status())) {
            wrapper.eq(Visit::getStatus, query.status());
        }
        Page<Visit> visitPage = visitMapper.selectPage(page, wrapper);
        return PageResponse.of(visitPage);
    }

    /**
     * 查询当前登录用户创建的预约。
     *
     * <p>实现提示：从 {@code CurrentUser.id()} 取用户编号后调用 {@link VisitMapper#selectMine(Long)}，不要把 userId 暴露成接口参数。
     *
     * @return 当前用户的预约列表
     */
    @Override
    public List<Visit> mine() {
        Long userId = currentUser.id();
        return visitMapper.selectMine(userId);
    }

    /**
     * 查询预约详情。
     *
     * <p>实现提示：先按主键查询，不存在返回 404；管理员/员工可以读取，宠物主人只能读取 {@code createdBy} 等于当前用户编号的记录，
     * 否则返回 403。
     *
     * @param id 预约编号
     * @return 预约详情
     */
    @Override
    public Visit get(Long id) {
        /*
         * 原代码：异常写法可以工作，但没有复用项目已有的 notFound 工厂方法。
         * if(visit==null){
         *     throw new BusinessException(HttpStatus.NOT_FOUND,"预约不存在");
         * }
         */
        Visit visit = visitMapper.selectById(id);
        if (visit == null) {
            throw BusinessException.notFound("预约");
        }
        AuthenticatedUser user = currentUser.require();

        /*
         * 原代码：直接调用 equals，字段意外为 null 时可能抛出空指针。
         * if(user.getAccountType().equals("OWNER")){
         *     if(!visit.getCreatedBy().equals(user.getId())) {
         *         throw new AccessDeniedException("不能查询其他宠物的预约信息");
         *     }
         * }
         */
        if ("OWNER".equals(user.getAccountType())) {
            if (!Objects.equals(visit.getCreatedBy(), user.getId())) {
                throw new AccessDeniedException("不能查询其他用户的预约信息");
            }
        }
        return visit;
    }

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
     * @param id      预约编号
     * @param request 取消原因
     * @return 已取消的预约
     */
    @Override
    @Transactional
    public Visit cancel(Long id, CancelVisitRequest request) {
        /*
         * 原代码：没有复用统一的 404 工厂方法。
         * if(visit==null){
         *     throw new BusinessException(HttpStatus.NOT_FOUND,"预约不存在");
         * }
         */
        Visit visit = visitMapper.selectById(id);
        if (visit == null) {
            throw BusinessException.notFound("预约");
        }
        AuthenticatedUser user = currentUser.require();

        /*
         * 原代码：直接 equals，在异常数据为 null 时不够安全。
         * if(!user.getId().equals(visit.getCreatedBy())){
         *     throw new AccessDeniedException("无权取消他人预约");
         * }
         */
        if (!Objects.equals(user.getId(), visit.getCreatedBy())) {
            throw new AccessDeniedException("无权取消他人预约");
        }

        /*
         * 原代码：SCHEDULED 前多写了一个空格，导致正常预约也无法取消；异常构造参数也无法编译。
         * if(!" SCHEDULED".equals(visit.getStatus())){
         *     throw new BusinessException(409,"状态非法");
         * }
         */
        if (!"SCHEDULED".equals(visit.getStatus())) {
            throw BusinessException.conflict("只有待就诊预约可以取消");
        }

        /*
         * 原代码：两次分别取时间，且修改对象后没有调用 updateById，数据库中的预约不会变成 CANCELLED。
         * visit.setStatus("CANCELLED");
         * visit.setCancelledAt(LocalDateTime.now());
         * visit.setCancelReason(request.reason());
         * visit.setUpdatedAt(LocalDateTime.now());
         */
        LocalDateTime now = LocalDateTime.now();
        visit.setStatus("CANCELLED");
        visit.setCancelledAt(now);
        visit.setCancelReason(request.reason());
        visit.setUpdatedAt(now);
        if (visitMapper.updateById(visit) != 1) {
            throw BusinessException.conflict("预约状态更新失败，请重试");
        }

        /*
         * 原代码：释放失败时使用了项目中不存在的 BusinessException(String) 构造器。
         * int t = visitMapper.releaseSlot(visit.getSlotId());
         * if(t!=1){
         *     throw new BusinessException("受影响行数不只一行");
         * }
         */
        int t = visitMapper.releaseSlot(visit.getSlotId());
        if (t != 1) {
            throw BusinessException.conflict("预约时段状态异常，取消失败");
        }
        return visit;
    }

    /**
     * 将待就诊预约标记为已完成。
     *
     * <p>实现提示：该操作只应授权给员工或管理员。先查询预约，不存在返回 404；只有 SCHEDULED 能变为 COMPLETED，已取消或已完成返回
     * 409；更新状态和 {@code updatedAt} 后保存。完成就诊不释放时段，因为该时段已经实际使用。
     *
     * @param id 预约编号
     * @return 已完成的预约
    */
    @Override
    // 原代码中 complete 方法没有 @Transactional；下面按小模块保留原代码用于对比。
    @Transactional
    public Visit complete(Long id) {
        /*
         * 原代码：没有复用统一的 404 工厂方法。
         * if(visit==null){
         *     throw new BusinessException(HttpStatus.NOT_FOUND,"预约信息不存在");
         * }
         */
        Visit visit = visitMapper.selectById(id);
        if (visit == null) {
            throw BusinessException.notFound("预约");
        }

        /*
         * 原代码：状态判断正确，但 BusinessException 不接受 int 类型的状态码，代码无法编译。
         * if(!"SCHEDULED".equals(visit.getStatus())){
         *     throw new BusinessException(409,"状态非法");
         * }
         */
        if (!"SCHEDULED".equals(visit.getStatus())) {
            throw BusinessException.conflict("只有待就诊预约可以完成");
        }

        /*
         * 原代码：执行了更新，但没有检查受影响行数，更新失败时仍会返回成功对象。
         * visit.setStatus("COMPLETED");
         * visit.setUpdatedAt(LocalDateTime.now());
         * visitMapper.updateById(visit);
         */
        visit.setStatus("COMPLETED");
        visit.setUpdatedAt(LocalDateTime.now());
        if (visitMapper.updateById(visit) != 1) {
            throw BusinessException.conflict("预约状态更新失败，请重试");
        }
        return visit;
    }
}
