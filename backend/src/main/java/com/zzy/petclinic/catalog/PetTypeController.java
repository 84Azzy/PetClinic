package com.zzy.petclinic.catalog;

import com.zzy.petclinic.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "宠物类型")
@RestController
@RequestMapping("/api/pet-types")
@RequiredArgsConstructor
public class PetTypeController {
  private final CatalogService s;

  @GetMapping
  public ApiResponse<List<PetType>> list() {
    return ApiResponse.ok(s.petTypes());
  }

  @GetMapping("/{id}")
  public ApiResponse<PetType> get(@PathVariable Long id) {
    return ApiResponse.ok(s.petType(id));
  }

  @PostMapping
  public ApiResponse<PetType> create(@Valid @RequestBody CatalogRequest r) {
    return ApiResponse.created(s.createPetType(r));
  }

  @PutMapping("/{id}")
  public ApiResponse<PetType> update(@PathVariable Long id, @Valid @RequestBody CatalogRequest r) {
    return ApiResponse.ok(s.updatePetType(id, r));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    s.deletePetType(id);
    return ApiResponse.message("已删除");
  }
}
