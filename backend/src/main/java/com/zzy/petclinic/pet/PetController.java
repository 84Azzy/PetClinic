package com.zzy.petclinic.pet;

import com.zzy.petclinic.common.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "宠物档案（学习者实现）")
@RestController
@RequestMapping("/api/pets")
public class PetController {

  @Autowired
  private PetService petService;

  @Operation(summary = "新增宠物")
  @PostMapping
  @PreAuthorize("hasAuthority('pet:create') && hasAnyRole('ADMIN', 'STAFF')")
  public ApiResponse<Pet> create(@Valid @RequestBody PetRequest r) {
    return ApiResponse.created(petService.create(r));
  }

  @Operation(summary = "分页查询宠物")
  @GetMapping
  @PreAuthorize("hasAuthority('pet:manage')")
  public ApiResponse<PageResponse<Pet>> page(
      @Valid PageQuery q, @RequestParam(required = false) Long ownerId) {
    /*
     * 不恰当：Controller 根据 accountType 分支并执行资源归属判断，其他入口直接调用 Service 时可以绕过。
     * 原 page(...) 中的身份判断和 ownerId 收敛逻辑已下沉到 PetServiceImpl.page(...)。
     */
    return ApiResponse.ok(petService.page(q, ownerId));
  }

  @Operation(summary = "查询我的宠物，宠物主人专用")
  @GetMapping("/mine")
  @PreAuthorize("hasRole('OWNER')")
  public ApiResponse<List<Pet>> mine() {
    // 不恰当：把 principal 的 userId 从 Controller 传入 Service，Service 仍可能被其他调用方传入任意用户编号。
    // return ApiResponse.ok(petService.mine(user.getId()));
    return ApiResponse.ok(petService.mine());
  }

  @Operation(summary = "查询宠物详情")
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('pet:manage')")
  public ApiResponse<Pet> get(@PathVariable Long id) {
    return ApiResponse.ok(petService.get(id));
  }

  @Operation(summary = "修改宠物")
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('pet:update') && hasAnyRole('ADMIN', 'STAFF')")
  public ApiResponse<Pet> update(@PathVariable Long id, @Valid @RequestBody PetRequest r) {
    return ApiResponse.ok(petService.update(id,r));
  }

  @Operation(summary = "停用宠物")
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('pet:delete') && hasAnyRole('ADMIN', 'STAFF')")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    petService.delete(id);
    return ApiResponse.message("删除成功");
  }

}
