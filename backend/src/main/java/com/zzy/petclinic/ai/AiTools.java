package com.zzy.petclinic.ai;

import com.zzy.petclinic.catalog.CatalogService;
import com.zzy.petclinic.catalog.Specialty;
import com.zzy.petclinic.common.BusinessException;
import com.zzy.petclinic.common.PageQuery;
import com.zzy.petclinic.pet.Pet;
import com.zzy.petclinic.pet.PetService;
import com.zzy.petclinic.rbac.authentication.CurrentUser;
import com.zzy.petclinic.schedule.ScheduleService;
import com.zzy.petclinic.schedule.VetScheduleSlot;
import com.zzy.petclinic.vet.Vet;
import com.zzy.petclinic.vet.VetService;
import com.zzy.petclinic.visit.Visit;
import com.zzy.petclinic.visit.VisitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * 暴露给 AI 模型的只读业务工具集。
 *
 * <p>所有工具只调用业务 Service，不直接依赖 Mapper；用户身份始终从 {@link CurrentUser}
 * 获取，不暴露为模型可传入的参数。该工具集只负责查询，不创建、取消或修改预约。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiTools {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");

    private final CurrentUser currentUser;
    private final PetService petService;
    private final VetService vetService;
    private final CatalogService catalogService;
    private final ScheduleService scheduleService;
    private final VisitService visitService;
    private final AiToolCallRecorder recorder;

    /**
     * 查询当前登录宠物主人的有效宠物。
     *
     * @return 包含查询状态和宠物摘要的工具结果
     */
    @Tool(
            name = "listMyPets",
            description = "查询当前宠物主人自己的有效宠物，不接收userid;不得用于查询其他用户宠物"
    )
    public ToolResult<List<PetView>> listMyPets(){
        return query(
                "listMyPets",
                ()->petService.mine().stream().map(PetView::from).toList()
        );
    }

    /**
     * 按姓名关键词和专科中文名称查询在职兽医。
     *
     * @param keyword 兽医姓名关键词，可为 {@code null}
     * @param specialtyName 专科中文名称，可为 {@code null}
     * @return 包含查询状态和兽医摘要的工具结果
     */
    @Tool(name="findVets",
    description = "按兽医姓名关键词或者专科名称查询在职兽医，keyword和specialtyName都可以省略，但不得伪造专科编号")
    public ToolResult<List<VetView>> findVets(
            @ToolParam(description = "兽医姓名关键词，例如王医生，可省略",required = false)String keyword,
            @ToolParam(description = "专科中文名称，例如外科，可以省略",required = false)String specialtyName
    ){
        return query(
                "findVets",
                ()->{
                    Long specialtyId = null;
                    if(StringUtils.hasText(specialtyName)){
                        String wanted = specialtyName.trim();
                        Specialty specialty = catalogService.specialties().stream()
                                // 不恰当：x.getStatus() 为 null 时会抛 NullPointerException。
                                // .filter(x->x.getStatus().equals("ACTIVE"))
                                .filter(x->"ACTIVE".equals(x.getStatus()))
                                .filter(x->x.getName().contains(wanted)
                                || wanted.contains(x.getName()))
                                .findFirst()
                                .orElseThrow(()->BusinessException.notFound("专科"));
                        specialtyId=specialty.getId();
                    }
                    PageQuery page = new PageQuery(1L,20L,StringUtils.hasText(keyword)?keyword.trim():null,"ACTIVE");
                    return vetService.page(page,specialtyId).records().stream()
                            .map(VetView::fromVet)
                            .toList();
                }
        );
    }

    /**
     * 查询指定兽医在某日尚未过期的可预约时段。
     *
     * @param vetId 由 {@link #findVets(String, String)} 获得的真实兽医编号
     * @param date yyyy-MM-dd 格式的日期
     * @return 包含查询状态和时段摘要的工具结果
     */
    @Tool(
            name= "findAvailableSlots",
            description = "查询指定兽医在指定日期仍可预约的时段。必须先知道真实的vetId和日期，缺少时应先向用户追问"
    )
    public ToolResult<List<SlotView>> findAvailableSlots(
            // 不恰当：vetId 是查询时段的必需参数，不应标成 required = false。
            // @ToolParam(description = "由findVets查询得到真实兽医编号",required = false)Long vetId,
            @ToolParam(description = "由findVets查询得到真实兽医编号",required = true)Long vetId,
            @ToolParam(description = "日期，严格使用yyyy-MM-dd格式",required = true)String date
    ){
        return query(
                "findAvailableSlots",
                ()->{
                    // required = true 只会告诉模型该参数必填，Java 边界仍需要自己防御 null。
                    if(vetId==null){
                        throw BusinessException.badRequest("vetId不能为空");
                    }
                    if(!StringUtils.hasText(date)){
                        throw BusinessException.badRequest("date不能为空，且必须使用yyyy-MM-dd格式");
                    }
                    LocalDate parseDate;
                    try{
                        parseDate = LocalDate.parse(date.trim());
                    }catch (DateTimeParseException e){
                        throw BusinessException.badRequest("date必须使用yyyy-MM-dd格式");
                    }
                    if(parseDate.isBefore(LocalDate.now(APP_ZONE))){
                        throw BusinessException.badRequest("不能查询过去日期的可预约时段");
                    }
                    Vet vet=vetService.get(vetId);
                    if(!"ACTIVE".equals(vet.getStatus())){
                        throw BusinessException.conflict("该兽医当前已停用");
                    }
                    return scheduleService.available(vetId,parseDate)
                            .stream()
                            // 不恰当：只判断 AVAILABLE 会把当天已过期的时段也返回给模型。
                            // .map(SlotView::from)
                            .filter(x->x.getStartTime()!=null
                                    && x.getStartTime().isAfter(LocalDateTime.now(APP_ZONE)))
                            .map(SlotView::from)
                            .toList();
                }
        );
    }

    /**
     * 查询当前登录用户自己创建的预约。
     *
     * @return 包含查询状态和预约摘要的工具结果
     */
    @Tool(
            name = "listMyVisits",
            description = "查询当前登录用户自己创建的预约记录。不接受userId,不得查询其他用户的预约"
    )
    public ToolResult<List<VisitView>> listMyVisits(){
        return query(
                "listMyVisits",
                ()->visitService.mine().stream()
                        .map(VisitView::from)
                        .toList()
        );
    }


    /**
     * 统一执行只读工具，并记录成功状态、耗时和结果数量。
     *
     * <p>可预期的业务/权限异常会被转成 {@link ToolResult} 交给模型处理；未知运行时异常仍向上抛出，
     * 交由模型网关转换为统一错误。
     *
     * @param toolName 工具名称
     * @param action 真正的 Service 查询逻辑
     * @param <T> 查询数据类型
     * @return 模型可理解的统一工具结果
     */
    private <T> ToolResult<T> query(String toolName, Supplier<T> action){
        long start = System.nanoTime();
        try{
            /*
             * 原 TODO 解答：这里不是为了使用 id 数值，而是借助 CurrentUser.id()
             * 的校验副作用确认 SecurityContext 中存在真实登录用户。findVets
             * 和 findAvailableSlots 本身不按用户过滤，所以统一入口仍需要这道认证守卫。
             * 不把返回值保存成局部变量，可以更清楚地表达「只做校验」。
             */
            currentUser.id();
            T data = action.get();
            int count = data instanceof Collection<?> collection
                    ?collection.size():data==null?0:1;
            long durationMs = (System.nanoTime()-start)/1_000_000;
            recorder.record(toolName,true,durationMs,count);
            log.info(
                    "AI tool={} success=true durationMs={} resultCount={}",
                    toolName,durationMs,count
            );
            return new ToolResult<>(true,"查询成功",data);
        }catch (BusinessException | AccessDeniedException exception){
            long durationMs = (System.nanoTime()-start)/1_000_000;
            recorder.record(toolName,false,durationMs,0);
            log.warn(
                    "AI tool={} success=false durationMs={} errorType={}",
                    toolName,durationMs,exception.getClass().getSimpleName()
            );
            return new ToolResult<>(false, exception.getMessage(), null);
        } catch (RuntimeException e) {
            long durationMs = (System.nanoTime()-start)/1_000_000;
            recorder.record(toolName,false,durationMs,0);
            log.error(
                    "AI tool={} success=false durationMs={} errorType={}",
                    toolName,durationMs,e.getClass().getSimpleName()
            );
            throw e;
        }
    }

    /**
     * AI 工具的统一返回包装。
     *
     * @param success 查询是否成功
     * @param message 供模型理解的简短说明
     * @param data 成功时的最小化业务数据
     * @param <T> 数据类型
     */
    public record ToolResult<T>(
            boolean success,
            String message,
            T data){}

    /**
     * 供模型使用的宠物最小视图，不暴露过敏史等敏感详情。
     *
     * @param id 宠物编号
     * @param name 宠物名称
     * @param typeId 宠物类型编号
     * @param gender 性别
     * @param breed 品种
     * @param status 状态
     */
    public record PetView(
            Long id,
            String name,
            // 不恰当：record 组件首字母大写会生成 TypeId() 并导致 JSON 字段命名不符合约定。
            // Long TypeId,
            Long typeId,
            String gender,
            String breed,
            String status){
        /**
         * 从完整宠物实体提取供模型使用的最小字段。
         *
         * @param x 宠物实体
         * @return 宠物工具视图
         */
        static PetView from(Pet x){
            return new PetView(
                    x.getId(),
                    x.getName(),
                    x.getTypeId(),
                    x.getGender(),
                    x.getBreed(),
                    x.getStatus());
        }
    }

    /**
     * 供模型使用的兽医摘要。
     *
     * @param id 兽医编号
     * @param name 兽医姓名
     * @param biography 个人简介
     * @param status 状态
     */
    public record VetView(
            Long id,
            String name,
            String biography,
            String status){
        /**
         * 从兽医实体提取模型需要的字段。
         *
         * @param x 兽医实体
         * @return 兽医工具视图
         */
        static VetView fromVet(Vet x){
            return new VetView(
                    x.getId(),
                    x.getName(),
                    x.getBiography(),
                    x.getStatus());
        }
    }

    /**
     * 供模型使用的可预约时段摘要。
     *
     * @param id 时段编号
     * @param vetId 兽医编号
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param status 状态
     */
    public record SlotView(
            Long id,
            Long vetId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String status) {

        /**
         * 从排班实体提取模型需要的字段。
         *
         * @param x 排班时段实体
         * @return 时段工具视图
         */
        static SlotView from(VetScheduleSlot x) {
            return new SlotView(
                    x.getId(),
                    x.getVetId(),
                    x.getStartTime(),
                    x.getEndTime(),
                    x.getStatus());
        }
    }

    /**
     * 供模型使用的预约摘要。
     *
     * @param id 预约编号
     * @param petId 宠物编号
     * @param slotId 时段编号
     * @param vetId 兽医编号
     * @param reason 就诊原因
     * @param status 预约状态
     */
    public record VisitView(
            Long id,
            Long petId,
            Long slotId,
            Long vetId,
            String reason,
            String status) {

        /**
         * 从预约实体提取模型需要的字段。
         *
         * @param x 预约实体
         * @return 预约工具视图
         */
        static VisitView from(Visit x) {
            return new VisitView(
                    x.getId(),
                    x.getPetId(),
                    x.getSlotId(),
                    x.getVetId(),
                    x.getReason(),
                    x.getStatus());
        }
    }
}
