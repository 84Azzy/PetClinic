package com.zzy.petclinic.rbac.system;

import com.zzy.petclinic.common.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;

import lombok.RequiredArgsConstructor;
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
  @PutMapping("/{id}")
  public ApiResponse<SysRole> update(
      @PathVariable Long id, @Valid @RequestBody SystemRequests.RoleSave r) {
    return ApiResponse.ok(roleService.update(id, r));
  }

  /** 删除角色；关联校验与清理必须在 Service 的事务中完成。 */
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    roleService.delete(id);
    return ApiResponse.message("删除角色成功");
  }

  /** 全量替换角色拥有的权限。 */
  @PutMapping("/{id}/permissions")
  public ApiResponse<Void> permissions(
      @PathVariable Long id, @Valid @RequestBody SystemRequests.Ids r) {
    roleService.assignPermissions(id, r.ids());
    return ApiResponse.message("角色权限替换成功");
  }

  private FeatureNotImplementedException todo() {
    return new FeatureNotImplementedException("RBAC 角色与权限分配");
  }
}
