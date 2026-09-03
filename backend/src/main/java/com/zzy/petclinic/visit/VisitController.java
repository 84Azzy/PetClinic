package com.zzy.petclinic.visit;

import com.zzy.petclinic.common.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 预约接口入口。
 *
 * <p>本类只负责接收并校验 HTTP 参数、调用 {@link VisitService}、包装 {@link ApiResponse}；事务、权限、归属和状态流转等业务规则统一
 * 放在 Service 实现中，避免其他入口绕过规则。
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
  @Operation(summary = "创建预约：包含幂等、归属校验和时段条件抢占")
  @PostMapping
  @PreAuthorize("hasAuthority('visit:create')")
  public ApiResponse<Visit> create(@Valid @RequestBody VisitRequest r) {
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
  @PreAuthorize("hasAuthority('visit:manage')")
  public ApiResponse<PageResponse<Visit>> page(
      @Valid PageQuery q,
      @RequestParam(required = false) Long petId,
      @RequestParam(required = false) Long vetId) {
    return ApiResponse.ok(visitService.page(q, petId, vetId));
  }

  /**
   * 查询当前登录用户创建的预约。
   *
   * @return 当前用户的预约列表
   */
  @GetMapping("/mine")
  @PreAuthorize("hasAuthority('visit:manage')")
  public ApiResponse<List<Visit>> mine() {
    return ApiResponse.ok(visitService.mine());
  }

  /**
   * 查询单条预约详情。
   *
   * @param id 预约编号
   * @return 预约详情
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('visit:manage')")
  public ApiResponse<Visit> get(@PathVariable Long id) {
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
  @PreAuthorize("hasAuthority('visit:cancel')")
  public ApiResponse<Visit> cancel(
      @PathVariable Long id, @Valid @RequestBody CancelVisitRequest r) {
    return ApiResponse.ok(visitService.cancel(id, r));
  }

  /**
   * 将待就诊预约标记为已完成。
   *
   * @param id 预约编号
   * @return 完成后的预约
   */
  @PostMapping("/{id}/complete")
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
  public ApiResponse<Visit> complete(@PathVariable Long id) {
    return ApiResponse.ok(visitService.complete(id));
  }
}
