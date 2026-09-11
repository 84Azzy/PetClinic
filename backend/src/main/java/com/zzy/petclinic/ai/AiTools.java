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
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiTools {

    private final CurrentUser currentUser;
    private final PetService petService;
    private final VetService vetService;
    private final CatalogService catalogService;
    private final ScheduleService scheduleService;
    private final VisitService visitService;
    private final AiToolCallRecorder recorder;

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
                                .filter(x->x.getStatus().equals("ACTIVE"))
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

    @Tool(
            name= "findAvailableSlots",
            description = "查询指定兽医在指定日期仍可预约的时段。必须先知道真实的vetId和日期，缺少时应先向用户追问"
    )
    public ToolResult<List<SlotView>> findAvailableSlots(
            @ToolParam(description = "由findVets查询得到真实兽医编号",required = false)Long vetId,
            @ToolParam(description = "日期，严格使用yyyy-MM-dd格式",required = true)String date
    ){
        return query(
                "findAvailableSlots",
                ()->{
                    LocalDate parseDate;
                    try{
                        parseDate = LocalDate.parse(date);
                    }catch (DateTimeParseException e){
                        throw BusinessException.badRequest("date必须使用yyyy-MM-dd格式");
                    }
                    Vet vet=vetService.get(vetId);
                    if(!"ACTIVE".equals(vet.getStatus())){
                        throw BusinessException.conflict("该兽医当前已停用");
                    }
                    return scheduleService.available(vetId,parseDate)
                            .stream()
                            .map(SlotView::from)
                            .toList();
                }
        );
    }

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


    private <T> ToolResult<T> query(String toolName, Supplier<T> action){
        long start = System.nanoTime();
        try{
            //todo id查询出来在这里没有明显使用，为啥要查它
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

    public record ToolResult<T>(
            boolean success,
            String message,
            T data){}

    public record PetView(
            Long id,
            String name,
            Long TypeId,
            String gender,
            String breed,
            String status){
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

    public record VetView(
            Long id,
            String name,
            String biography,
            String status){
        static VetView fromVet(Vet x){
            return new VetView(
                    x.getId(),
                    x.getName(),
                    x.getBiography(),
                    x.getStatus());
        }
    }

    public record SlotView(
            Long id,
            Long vetId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String status) {

        static SlotView from(VetScheduleSlot x) {
            return new SlotView(
                    x.getId(),
                    x.getVetId(),
                    x.getStartTime(),
                    x.getEndTime(),
                    x.getStatus());
        }
    }

    public record VisitView(
            Long id,
            Long petId,
            Long slotId,
            Long vetId,
            String reason,
            String status) {

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
