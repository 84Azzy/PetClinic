package com.zzy.petclinic.rbac.system;

import com.zzy.petclinic.rbac.authentication.SysUser;
import com.zzy.petclinic.common.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * 系统用户管理 HTTP 入口。
 *
 * <p>待 {@link SystemServices.UserAdminService} 有实现后，通过构造器注入它，并用 {@code @PreAuthorize} 限制为具有系统管理权限的用户。
 * 本类只负责接收参数、调用 Service 和包装 {@link ApiResponse}；密码哈希、唯一性、事务及 token 失效都属于 Service 职责。
 */
@Tag(name = "RBAC-用户管理（学习者实现）")
@RestController
@RequestMapping("/api/system/users")
public class UserAdminController {
  /** 查询用户分页。 */
  @GetMapping
  public ApiResponse<PageResponse<SysUser>> page(@Valid PageQuery q) {
    // TODO 接线：return ApiResponse.ok(userAdminService.page(q));
    throw todo();
  }

  /** 新建用户，成功时应使用 ApiResponse.created(...) 返回。 */
  @PostMapping
  public ApiResponse<SysUser> create(@Valid @RequestBody SystemRequests.UserSave r) {
    // TODO 接线：return ApiResponse.created(userAdminService.create(r));
    throw todo();
  }

  /** 修改用户基本资料；不要在 Controller 中直接操作 Mapper。 */
  @PutMapping("/{id}")
  public ApiResponse<SysUser> update(
      @PathVariable Long id, @Valid @RequestBody SystemRequests.UserSave r) {
    // TODO 接线：return ApiResponse.ok(userAdminService.update(id, r));
    throw todo();
  }

  /** 启用或停用账号。 */
  @PatchMapping("/{id}/status")
  public ApiResponse<Void> status(
      @PathVariable Long id, @Valid @RequestBody SystemRequests.StatusChange r) {
    // TODO 接线：调用 userAdminService.changeStatus(id, r.status())，再返回成功消息。
    throw todo();
  }

  /** 管理员重置用户密码。 */
  @PutMapping("/{id}/password")
  public ApiResponse<Void> password(
      @PathVariable Long id, @Valid @RequestBody SystemRequests.PasswordReset r) {
    // TODO 接线：调用 userAdminService.resetPassword(id, r.password())，再返回成功消息。
    throw todo();
  }

  /** 全量替换指定用户的角色。 */
  @PutMapping("/{id}/roles")
  public ApiResponse<Void> roles(@PathVariable Long id, @Valid @RequestBody SystemRequests.Ids r) {
    // TODO 接线：调用 userAdminService.assignRoles(id, r.ids())，再返回成功消息。
    throw todo();
  }

  /** 查询指定用户已经拥有的角色。 */
  @GetMapping("/{id}/roles")
  public ApiResponse<List<SysRole>> roles(@PathVariable Long id) {
    // TODO 接线：return ApiResponse.ok(userAdminService.roles(id));
    throw todo();
  }

  private FeatureNotImplementedException todo() {
    return new FeatureNotImplementedException("RBAC 用户管理与角色分配");
  }
}
