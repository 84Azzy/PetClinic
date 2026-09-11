package com.zzy.petclinic.rbac.system.controller;

import com.zzy.petclinic.rbac.authentication.SysUser;
import com.zzy.petclinic.common.*;
import com.zzy.petclinic.rbac.system.dataObject.SysRole;
import com.zzy.petclinic.rbac.system.dataObject.SystemRequests;
import com.zzy.petclinic.rbac.system.systemServices.SystemServices;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 系统用户管理 HTTP 入口。
 *
 * <p>当前入口通过构造器注入 {@link SystemServices.UserAdminService}，并用 {@code @PreAuthorize}
 * 限制为具有系统管理权限的用户。本类只负责接收参数、调用 Service 和包装 {@link ApiResponse}；密码哈希、唯一性、事务及 token
 * 失效都属于 Service 职责。
 */
@Tag(name = "RBAC-用户管理（学习者实现）")
@RestController
@RequestMapping("/api/system/users")
@PreAuthorize("hasAuthority('system:manage')")
@RequiredArgsConstructor
public class UserAdminController {
  private final SystemServices.UserAdminService userAdminService;

  /** 查询用户分页。 */
  @GetMapping
  public ApiResponse<PageResponse<SysUser>> page(@Valid PageQuery q) {
    return ApiResponse.ok(userAdminService.page(q));
  }

  /** 新建用户，成功时应使用 ApiResponse.created(...) 返回。 */
  @PostMapping
  public ApiResponse<SysUser> create(@Valid @RequestBody SystemRequests.UserSave r) {
    return ApiResponse.created(userAdminService.create(r));
  }

  /** 修改用户基本资料；不要在 Controller 中直接操作 Mapper。 */
  @PutMapping("/{id}")
  public ApiResponse<SysUser> update(
      @PathVariable Long id, @Valid @RequestBody SystemRequests.UserSave r) {
    return ApiResponse.ok(userAdminService.update(id, r));
  }

  /** 启用或停用账号。 */
  @PatchMapping("/{id}/status")
  public ApiResponse<Void> status(
      @PathVariable Long id, @Valid @RequestBody SystemRequests.StatusChange r) {
    userAdminService.changeStatus(id, r.status());
    return ApiResponse.message("用户状态修改成功");
  }

  /** 管理员重置用户密码。 */
  @PutMapping("/{id}/password")
  public ApiResponse<Void> password(
      @PathVariable Long id, @Valid @RequestBody SystemRequests.PasswordReset r) {
    userAdminService.resetPassword(id, r.password());
    return ApiResponse.message("用户密码重置成功");
  }

  /** 全量替换指定用户的角色。 */
  @PutMapping("/{id}/roles")
  public ApiResponse<Void> roles(@PathVariable Long id, @Valid @RequestBody SystemRequests.Ids r) {
    userAdminService.assignRoles(id, r.ids());
    return ApiResponse.message("用户角色分配成功");
  }

  /** 查询指定用户已经拥有的角色。 */
  @GetMapping("/{id}/roles")
  public ApiResponse<List<SysRole>> roles(@PathVariable Long id) {
    return ApiResponse.ok(userAdminService.roles(id));
  }
}
