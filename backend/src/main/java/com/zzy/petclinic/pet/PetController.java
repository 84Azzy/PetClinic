package com.zzy.petclinic.pet;

import com.zzy.petclinic.common.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@Tag(name = "宠物档案（学习者实现）")
@RestController
@RequestMapping("/api/pets")
public class PetController {
  @Operation(summary = "新增宠物")
  @PostMapping
  public ApiResponse<Pet> create(@Valid @RequestBody PetRequest r) {
    throw todo();
  }

  @Operation(summary = "分页查询宠物")
  @GetMapping
  public ApiResponse<PageResponse<Pet>> page(
      @Valid PageQuery q, @RequestParam(required = false) Long ownerId) {
    throw todo();
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
