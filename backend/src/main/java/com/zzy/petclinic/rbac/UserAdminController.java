package com.zzy.petclinic.rbac;

import com.zzy.petclinic.auth.SysUser;
import com.zzy.petclinic.common.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@Tag(name = "RBAC-用户管理（学习者实现）")
@RestController
@RequestMapping("/api/system/users")
public class UserAdminController {
  @GetMapping
  public ApiResponse<PageResponse<SysUser>> page(@Valid PageQuery q) {
    throw todo();
  }

  @PostMapping
  public ApiResponse<SysUser> create(@Valid @RequestBody RbacRequests.UserSave r) {
    throw todo();
  }

  @PutMapping("/{id}")
  public ApiResponse<SysUser> update(
      @PathVariable Long id, @Valid @RequestBody RbacRequests.UserSave r) {
    throw todo();
  }

  @PatchMapping("/{id}/status")
  public ApiResponse<Void> status(
      @PathVariable Long id, @Valid @RequestBody RbacRequests.StatusChange r) {
    throw todo();
  }

  @PutMapping("/{id}/password")
  public ApiResponse<Void> password(
      @PathVariable Long id, @Valid @RequestBody RbacRequests.PasswordReset r) {
    throw todo();
  }

  @PutMapping("/{id}/roles")
  public ApiResponse<Void> roles(@PathVariable Long id, @Valid @RequestBody RbacRequests.Ids r) {
    throw todo();
  }

  @GetMapping("/{id}/roles")
  public ApiResponse<List<SysRole>> roles(@PathVariable Long id) {
    throw todo();
  }

  private FeatureNotImplementedException todo() {
    return new FeatureNotImplementedException("RBAC 用户管理与角色分配");
  }
}
