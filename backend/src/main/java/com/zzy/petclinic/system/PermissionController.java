package com.zzy.petclinic.system;

import com.zzy.petclinic.common.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@Tag(name = "RBAC-权限管理（学习者实现）")
@RestController
@RequestMapping("/api/system/permissions")
public class PermissionController {
  @GetMapping
  public ApiResponse<List<SysPermission>> tree() {
    throw todo();
  }

  @GetMapping("/mine")
  public ApiResponse<List<SysPermission>> mine() {
    throw todo();
  }

  @PostMapping
  public ApiResponse<SysPermission> create(@Valid @RequestBody SystemRequests.PermissionSave r) {
    throw todo();
  }

  @PutMapping("/{id}")
  public ApiResponse<SysPermission> update(
      @PathVariable Long id, @Valid @RequestBody SystemRequests.PermissionSave r) {
    throw todo();
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    throw todo();
  }

  private FeatureNotImplementedException todo() {
    return new FeatureNotImplementedException("RBAC 权限树与后端授权");
  }
}
