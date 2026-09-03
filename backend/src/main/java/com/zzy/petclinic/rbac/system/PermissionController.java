package com.zzy.petclinic.rbac.system;

import com.zzy.petclinic.common.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * 权限节点及当前用户菜单权限的 HTTP 入口。
 *
 * <p>待 {@link SystemServices.PermissionService} 完成后用构造器注入。完整权限维护接口需要系统管理权限；{@code /mine} 只要求登录，并始终从
 * 服务端认证上下文取当前用户，不能接受客户端传入的 userId。
 */
@Tag(name = "RBAC-权限管理（学习者实现）")
@RestController
@RequestMapping("/api/system/permissions")
public class PermissionController {
  /** 查询完整权限树，供后台权限管理使用。 */
  @GetMapping
  public ApiResponse<List<SysPermission>> tree() {
    // TODO 接线：return ApiResponse.ok(permissionService.tree());
    throw todo();
  }

  /** 查询当前登录用户拥有的权限树，通常供前端生成菜单和按钮。 */
  @GetMapping("/mine")
  public ApiResponse<List<SysPermission>> mine() {
    // TODO 接线：return ApiResponse.ok(permissionService.mine());
    throw todo();
  }

  /** 新建权限节点。 */
  @PostMapping
  public ApiResponse<SysPermission> create(@Valid @RequestBody SystemRequests.PermissionSave r) {
    // TODO 接线：return ApiResponse.created(permissionService.create(r));
    throw todo();
  }

  /** 修改权限节点，并在 Service 中防止父子关系成环。 */
  @PutMapping("/{id}")
  public ApiResponse<SysPermission> update(
      @PathVariable Long id, @Valid @RequestBody SystemRequests.PermissionSave r) {
    // TODO 接线：return ApiResponse.ok(permissionService.update(id, r));
    throw todo();
  }

  /** 删除权限节点；子节点和角色关联由 Service 处理。 */
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    // TODO 接线：调用 permissionService.delete(id)，再返回成功消息。
    throw todo();
  }

  private FeatureNotImplementedException todo() {
    return new FeatureNotImplementedException("RBAC 权限树与后端授权");
  }
}
