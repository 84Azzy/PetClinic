package com.zzy.petclinic.pet;

import com.zzy.petclinic.rbac.authentication.AuthenticatedUser;
import com.zzy.petclinic.common.*;
import com.zzy.petclinic.owner.Owner;
import com.zzy.petclinic.owner.OwnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "宠物档案（学习者实现）")
@RestController
@RequestMapping("/api/pets")
public class PetController {

  @Autowired
  private PetService petService;
  @Autowired
  private OwnerService ownerService;

  @Operation(summary = "新增宠物")
  @PostMapping
  //TODO 需admin/staff级权限
  public ApiResponse<Pet> create(@Valid @RequestBody PetRequest r) {
    return ApiResponse.created(petService.create(r));
  }

  @Operation(summary = "分页查询宠物")
  @GetMapping
  public ApiResponse<PageResponse<Pet>> page(
          @Valid PageQuery q,
          @RequestParam(required = false) Long ownerId,
          @AuthenticationPrincipal AuthenticatedUser user) {
    if (user == null || user.getId() == null) {
      throw new AuthenticationCredentialsNotFoundException("请先登录或登录状态已失效");
    }

    String accountType = user.getAccountType();
    if ("ADMIN".equals(accountType) || "STAFF".equals(accountType)) {
      return ApiResponse.ok(petService.page(q, ownerId));
    }

    if ("OWNER".equals(accountType)) {
      Owner owner = ownerService.mine();
      Long currentOwnerId = owner.getId();
      if (ownerId != null && !ownerId.equals(currentOwnerId)) {
        throw new AccessDeniedException("不能查询其他宠物主人的宠物");
      }
      return ApiResponse.ok(petService.page(q, currentOwnerId));
    }

    throw new AccessDeniedException("当前账号类型无权查询宠物");
  }

  @Operation(summary = "查询我的宠物，宠物主人专用")
  @GetMapping("/mine")
  //TODO 需owner级权限
  public ApiResponse<List<Pet>> mine(@AuthenticationPrincipal AuthenticatedUser user) {
    if (user == null || user.getId() == null) {
      throw new AuthenticationCredentialsNotFoundException("请先登录或登录状态已失效");
    }
    return ApiResponse.ok(petService.mine(user.getId()));
  }

  @Operation(summary = "查询宠物详情")
  @GetMapping("/{id}")
  public ApiResponse<Pet> get(@PathVariable Long id) {
    return ApiResponse.ok(petService.get(id));
  }

  @Operation(summary = "修改宠物")
  @PutMapping("/{id}")
  //TODO 需admin/staff级权限
  public ApiResponse<Pet> update(@PathVariable Long id, @Valid @RequestBody PetRequest r) {
    return ApiResponse.ok(petService.update(id,r));
  }

  @Operation(summary = "停用宠物")
  @DeleteMapping("/{id}")
  //TODO 需admin/staff级权限
  public ApiResponse<Void> delete(@PathVariable Long id) {
    petService.delete(id);
    return ApiResponse.message("删除成功");
  }

}
