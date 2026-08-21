package com.zzy.petclinic.catalog;

import com.zzy.petclinic.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "兽医专长")
@RestController
@RequestMapping("/api/specialties")
@RequiredArgsConstructor
public class SpecialtyController {
  private final CatalogService s;

  @GetMapping
  public ApiResponse<List<Specialty>> list() {
    return ApiResponse.ok(s.specialties());
  }

  @GetMapping("/{id}")
  public ApiResponse<Specialty> get(@PathVariable Long id) {
    return ApiResponse.ok(s.specialty(id));
  }

  @PostMapping
  public ApiResponse<Specialty> create(@Valid @RequestBody CatalogRequest r) {
    return ApiResponse.created(s.createSpecialty(r));
  }

  @PutMapping("/{id}")
  public ApiResponse<Specialty> update(
      @PathVariable Long id, @Valid @RequestBody CatalogRequest r) {
    return ApiResponse.ok(s.updateSpecialty(id, r));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    s.deleteSpecialty(id);
    return ApiResponse.message("已删除");
  }
}
