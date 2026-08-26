package com.zzy.petclinic.pet;

import com.zzy.petclinic.authentication.AuthenticatedUser;
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
  public ApiResponse<Pet> create(@Valid @RequestBody PetRequest r) {
    throw todo();
  }

  /*
  @Operation(summary = "分页查询宠物")
  @GetMapping
  public ApiResponse<PageResponse<Pet>> page(
          @Valid PageQuery q,
          @RequestParam(required = false) Long ownerId,
          @AuthenticationPrincipal AuthenticatedUser user) {
    Long userId = user.getId();
    if(userId==null){
      //TODO是否真的需要抛出异常，需要抛出哪个异常
      throw new RuntimeException();
    }
    String accountType = user.getAccountType();
    //管理员、员工，如果传入ownerId，根据id查宠物
    if("ADMIN".equals(accountType) || "STAFF".equals(accountType)){
      return ApiResponse.ok(petService.page(q,ownerId));
    }
    else if("OWNER".equals(accountType)){   //如果为用户，owner.userId!=token中的userId，403越权
      Owner owner = ownerService.mine();
      if(!owner.getId().equals(ownerId)){
        //TODO应该是403，数据越权，但抛哪个异常，不知道名字
        throw new RuntimeException();
      }
      return ApiResponse.ok(petService.page(q,ownerId));
    }
    else{
      //TODO 同样的问题抛哪个异常
      throw new RuntimeException("用户账号状态异常");
    }
  }
  */

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

  @Operation(summary = "查询我的宠物")
  @GetMapping("/mine/{id}")
  public ApiResponse<List<Pet>> mine() {
    throw todo();
  }

  @Operation(summary = "查询宠物详情")
  @GetMapping("/{id}")
  public ApiResponse<Pet> get(@PathVariable Long id) {
    throw todo();
  }

  @Operation(summary = "修改宠物")
  @PutMapping("/{id}")
  public ApiResponse<Pet> update(@PathVariable Long id, @Valid @RequestBody PetRequest r) {
    throw todo();
  }

  @Operation(summary = "停用宠物")
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    throw todo();
  }

  private FeatureNotImplementedException todo() {
    return new FeatureNotImplementedException("Pet Controller → Service → Mapper 纵向链路");
  }
}
