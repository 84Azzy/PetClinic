package com.zzy.petclinic.rbac.system;

import com.zzy.petclinic.common.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 角色及角色权限分配的 HTTP 入口。
 *
 * <p>待 {@link SystemServices.RoleService} 完成后用构造器注入。Controller 不负责检查角色编码重复、关联数据或开启事务，这些规则全部放在
 * Service；本类已经用 {@code @PreAuthorize} 限制系统管理权限。
 */
@Tag(name = "RBAC-角色管理（学习者实现）")
@RestController
@RequestMapping("/api/system/roles")
@PreAuthorize("hasAuthority('system:manage')")
public class RoleController {
  /** 查询全部角色。 */
  @GetMapping
  public ApiResponse<List<SysRole>> list() {
    // TODO 接线：return ApiResponse.ok(roleService.list());
    throw todo();
  }

  /** 新建角色。 */
  @PostMapping
  public ApiResponse<SysRole> create(@Valid @RequestBody SystemRequests.RoleSave r) {
    // TODO 接线：return ApiResponse.created(roleService.create(r));
    throw todo();
  }

  /** 修改角色资料。 */
  @PutMapping("/{id}")
  public ApiResponse<SysRole> update(
      @PathVariable Long id, @Valid @RequestBody SystemRequests.RoleSave r) {
    // TODO 接线：return ApiResponse.ok(roleService.update(id, r));
    throw todo();
  }

  /** 删除角色；关联校验与清理必须在 Service 的事务中完成。 */
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    // TODO 接线：调用 roleService.delete(id)，再返回成功消息。
    throw todo();
  }

  /** 全量替换角色拥有的权限。 */
  @PutMapping("/{id}/permissions")
  public ApiResponse<Void> permissions(
      @PathVariable Long id, @Valid @RequestBody SystemRequests.Ids r) {
    // TODO 接线：调用 roleService.assignPermissions(id, r.ids())，再返回成功消息。
    throw todo();
  }

  private FeatureNotImplementedException todo() {
    return new FeatureNotImplementedException("RBAC 角色与权限分配");
  }
}
