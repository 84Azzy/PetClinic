package com.zzy.petclinic.visit;

import com.zzy.petclinic.common.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 预约接口入口。
 *
 * <p>实现时注入 {@link VisitService}。本类只负责接收并校验 HTTP 参数、调用 Service、包装 {@link ApiResponse}；事务、权限、归属和状态
 * 流转等业务规则统一放在 Service 实现中，避免其他入口绕过规则。
 */
@Tag(name = "预约就诊")
@RestController
@RequestMapping("/api/visits")
public class VisitController {

  @Autowired
  private VisitService visitService;
  /**
   * 创建预约。
   *
   * @param r 创建预约所需参数
   * @return HTTP 201 语义的预约响应
   */
  @Operation(summary = "创建预约：需实现幂等、归属校验和时段条件抢占")
  @PostMapping
  public ApiResponse<Visit> create(@Valid @RequestBody VisitRequest r) {
    // TODO 1. 给 Controller 注入 VisitService。
    // TODO 2. 调用 visitService.create(r)，不要在 Controller 中编写事务和抢占时段逻辑。
    // TODO 3. 使用 ApiResponse.created(...) 包装 Service 返回的预约。
   return ApiResponse.created(visitService.create(r));
  }

  /**
   * 分页查询预约，可按宠物和兽医进一步筛选。
   *
   * @param q 通用分页、关键词和状态参数
   * @param petId 可选的宠物编号
   * @param vetId 可选的兽医编号
   * @return HTTP 200 语义的分页响应
   */
  @GetMapping
  public ApiResponse<PageResponse<Visit>> page(
      @Valid PageQuery q,
      @RequestParam(required = false) Long petId,
      @RequestParam(required = false) Long vetId) {
    // TODO 调用 visitService.page(q, petId, vetId)，再用 ApiResponse.ok(...) 包装结果。
    // 用户只能看到自己数据的限制应由 Service 保证，不能依赖前端传参。
    return ApiResponse.ok(visitService.page(q,petId,vetId));
  }

  /**
   * 查询当前登录用户创建的预约。
   *
   * @return 当前用户的预约列表
   */
  @GetMapping("/mine")
  public ApiResponse<List<Visit>> mine() {
    // TODO 调用无需 userId 参数的 visitService.mine()，再用 ApiResponse.ok(...) 包装结果。
    // Service 会从认证上下文取得当前用户，Controller 不要接收客户端传入的 userId。
    return ApiResponse.ok(visitService.mine());
  }

  /**
   * 查询单条预约详情。
   *
   * @param id 预约编号
   * @return 预约详情
   */
  @GetMapping("/{id}")
  public ApiResponse<Visit> get(@PathVariable Long id) {
    // TODO 调用 visitService.get(id)，再用 ApiResponse.ok(...) 包装结果。
    // 预约是否存在、当前用户能否查看，由 Service 统一判断。
    return ApiResponse.ok(visitService.get(id));
  }

  /**
   * 取消预约。
   *
   * @param id 预约编号
   * @param r 取消原因
   * @return 取消后的预约
   */
  @Operation(summary = "取消预约并在同一事务释放时段")
  @PostMapping("/{id}/cancel")
  public ApiResponse<Visit> cancel(
      @PathVariable Long id, @Valid @RequestBody CancelVisitRequest r) {
    // TODO 调用 visitService.cancel(id, r)，再用 ApiResponse.ok(...) 包装结果。
    // 更新预约和释放时段必须在 Service 的同一事务中完成，Controller 不参与事务细节。
    return ApiResponse.ok(visitService.cancel(id,r));
  }

  /**
   * 将待就诊预约标记为已完成。
   *
   * @param id 预约编号
   * @return 完成后的预约
   */
  @PostMapping("/{id}/complete")
  public ApiResponse<Visit> complete(@PathVariable Long id) {
    // TODO 先为该接口配置员工/管理员权限，再调用 visitService.complete(id)，最后用 ApiResponse.ok(...) 包装结果。
    return ApiResponse.ok(visitService.complete(id));
  }
  
}
