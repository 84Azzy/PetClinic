package com.zzy.petclinic.system;

import com.zzy.petclinic.common.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@Tag(name = "RBAC-角色管理（学习者实现）")
@RestController
@RequestMapping("/api/system/roles")
public class RoleController {
  @GetMapping
  public ApiResponse<List<SysRole>> list() {
    throw todo();
  }

  @PostMapping
  public ApiResponse<SysRole> create(@Valid @RequestBody SystemRequests.RoleSave r) {
    throw todo();
  }

  @PutMapping("/{id}")
  public ApiResponse<SysRole> update(
      @PathVariable Long id, @Valid @RequestBody SystemRequests.RoleSave r) {
    throw todo();
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    throw todo();
  }

  @PutMapping("/{id}/permissions")
  public ApiResponse<Void> permissions(
      @PathVariable Long id, @Valid @RequestBody SystemRequests.Ids r) {
    throw todo();
  }

  private FeatureNotImplementedException todo() {
    return new FeatureNotImplementedException("RBAC 角色与权限分配");
  }
}
