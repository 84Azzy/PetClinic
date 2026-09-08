package com.zzy.petclinic.rbac.system;

import com.zzy.petclinic.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 角色及角色权限分配的 HTTP 入口。
 *
 * <p>通过构造器注入 {@link SystemServices.RoleService}。Controller 不负责检查角色编码重复、关联数据或开启事务，这些规则全部放在
 * Service；本类已经用 {@code @PreAuthorize} 限制系统管理权限。
 */
@Tag(name = "RBAC-角色管理（学习者实现）")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system/roles")
@PreAuthorize("hasAuthority('system:manage')")
public class RoleController {

  private final SystemServices.RoleService roleService;

  /** 查询全部角色。 */
  @GetMapping
  public ApiResponse<List<SysRole>> list() {
    return ApiResponse.ok(roleService.list());
  }

  /** 新建角色。 */
  @PostMapping
  public ApiResponse<SysRole> create(@Valid @RequestBody SystemRequests.RoleSave r) {
    return ApiResponse.created(roleService.create(r));
  }

  /** 修改角色资料。 */
  @PutMapping("/{roleId}")
  public ApiResponse<SysRole> update(
      @PathVariable Long roleId, @Valid @RequestBody SystemRequests.RoleSave r) {
    return ApiResponse.ok(roleService.update(roleId, r));
  }

  /** 删除角色；关联校验与清理必须在 Service 的事务中完成。 */
  // 不恰当：统一使用 id 容易让调用者误以为这里传的是 userId；该资源路径中的参数实际是 roleId。
  // @DeleteMapping("/{id}")
  // public ApiResponse<Void> delete(@PathVariable Long id) { ... }
  @DeleteMapping("/{roleId}")
  public ApiResponse<Void> delete(@PathVariable Long roleId) {
    roleService.delete(roleId);
    return ApiResponse.message("删除角色成功");
  }

  /** 全量替换角色拥有的权限。 */
  @PutMapping("/{roleId}/permissions")
  public ApiResponse<Void> permissions(
      @PathVariable Long roleId, @Valid @RequestBody SystemRequests.Ids r) {
    roleService.assignPermissions(roleId, r.ids());
    return ApiResponse.message("角色权限替换成功");
  }
}
